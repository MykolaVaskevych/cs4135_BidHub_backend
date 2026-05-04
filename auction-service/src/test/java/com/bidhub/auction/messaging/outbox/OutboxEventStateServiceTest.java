package com.bidhub.auction.messaging.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.bidhub.auction.messaging.outbox.OutboxEventStateService.ClaimedEvent;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class OutboxEventStateServiceTest {

    @Mock private OutboxEventRepository repo;

    private OutboxEventStateService service;

    @BeforeEach
    void setUp() {
        OutboxPublisherProperties props = new OutboxPublisherProperties();
        props.setMaxAttempts(3);
        props.setBatchSize(50);
        props.setProcessingTimeoutMs(60_000L);
        service = new OutboxEventStateService(repo, props);
    }

    @Test
    @DisplayName("claimBatch transitions PENDING rows to PROCESSING with attempts++")
    void claimBatch_pending_movesToProcessing() {
        OutboxEvent e = OutboxEvent.pending("X", "x.routing", "{}");
        when(repo.findForUpdateStaleByStatus(eq(OutboxStatus.PROCESSING), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of());
        when(repo.findForUpdateByStatus(eq(OutboxStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of(e));

        List<ClaimedEvent> claimed = service.claimBatch();

        assertThat(claimed).hasSize(1);
        assertThat(claimed.get(0).id()).isEqualTo(e.getId());
        assertThat(e.getStatus()).isEqualTo(OutboxStatus.PROCESSING);
        assertThat(e.getAttempts()).isEqualTo(1);
        assertThat(e.getProcessingStartedAt()).isNotNull();
    }

    @Test
    @DisplayName("claimBatch reclaims stale PROCESSING rows (processingStartedAt older than timeout)")
    void claimBatch_staleProcessing_isReclaimed() throws Exception {
        OutboxEvent stale = OutboxEvent.pending("STALE", "stale.routing", "{}");
        stale.markProcessing();
        setProcessingStartedAt(stale, Instant.now().minusSeconds(120));
        int attemptsBeforeReclaim = stale.getAttempts();

        when(repo.findForUpdateStaleByStatus(eq(OutboxStatus.PROCESSING), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(stale));
        when(repo.findForUpdateByStatus(eq(OutboxStatus.PENDING), any(Pageable.class)))
                .thenReturn(List.of());

        List<ClaimedEvent> claimed = service.claimBatch();

        assertThat(claimed).hasSize(1);
        assertThat(claimed.get(0).id()).isEqualTo(stale.getId());
        assertThat(stale.getStatus()).isEqualTo(OutboxStatus.PROCESSING);
        assertThat(stale.getAttempts()).isEqualTo(attemptsBeforeReclaim + 1);
        assertThat(stale.getProcessingStartedAt()).isAfter(Instant.now().minusSeconds(60));
    }

    @Test
    @DisplayName("claimBatch returns at most batchSize, prioritising stale before fresh PENDING")
    void claimBatch_staleFillsBudget_pendingNotQueriedWhenFull() {
        OutboxPublisherProperties props = new OutboxPublisherProperties();
        props.setBatchSize(1);
        props.setProcessingTimeoutMs(60_000L);
        OutboxEventStateService localService = new OutboxEventStateService(repo, props);
        OutboxEvent stale = OutboxEvent.pending("STALE", "x", "{}");
        stale.markProcessing();
        when(repo.findForUpdateStaleByStatus(eq(OutboxStatus.PROCESSING), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(stale));

        List<ClaimedEvent> claimed = localService.claimBatch();

        assertThat(claimed).hasSize(1);
        assertThat(claimed.get(0).id()).isEqualTo(stale.getId());
    }

    @Test
    @DisplayName("markPublished sets PUBLISHED + publishedAt")
    void markPublished_setsPublishedAt() {
        OutboxEvent e = OutboxEvent.pending("X", "x.routing", "{}");
        e.markProcessing();
        when(repo.findById(e.getId())).thenReturn(Optional.of(e));

        service.markPublished(e.getId());

        assertThat(e.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(e.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("handleFailure below max → PENDING with lastError")
    void handleFailure_belowMax_reQueues() {
        OutboxEvent e = OutboxEvent.pending("X", "x.routing", "{}");
        e.markProcessing();
        when(repo.findById(e.getId())).thenReturn(Optional.of(e));

        service.handleFailure(e.getId(), new AmqpException("broker unreachable"));

        assertThat(e.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(e.getLastError()).isEqualTo("broker unreachable");
    }

    @Test
    @DisplayName("handleFailure at max → FAILED")
    void handleFailure_atMax_marksFailed() {
        OutboxEvent e = OutboxEvent.pending("X", "x.routing", "{}");
        e.markProcessing();
        e.markPendingForRetry("attempt 1 fail");
        e.markProcessing();
        e.markPendingForRetry("attempt 2 fail");
        e.markProcessing();
        when(repo.findById(e.getId())).thenReturn(Optional.of(e));

        service.handleFailure(e.getId(), new AmqpException("still down"));

        assertThat(e.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(e.getLastError()).isEqualTo("still down");
    }

    @Test
    @DisplayName("handleFailure on missing event logs and returns without throwing")
    void handleFailure_missingEvent_swallowsAndLogs() {
        java.util.UUID id = java.util.UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.empty());

        service.handleFailure(id, new AmqpException("does not matter"));
    }

    private static void setProcessingStartedAt(OutboxEvent event, Instant value) throws Exception {
        Field f = OutboxEvent.class.getDeclaredField("processingStartedAt");
        f.setAccessible(true);
        f.set(event, value);
    }
}
