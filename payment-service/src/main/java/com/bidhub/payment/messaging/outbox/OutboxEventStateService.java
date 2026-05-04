package com.bidhub.payment.messaging.outbox;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxEventStateService {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventStateService.class);

    private final OutboxEventRepository repo;
    private final OutboxPublisherProperties properties;

    public OutboxEventStateService(
            OutboxEventRepository repo, OutboxPublisherProperties properties) {
        this.repo = repo;
        this.properties = properties;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<ClaimedEvent> claimBatch() {
        Instant staleBefore =
                Instant.now().minusMillis(properties.getProcessingTimeoutMs());
        int batchSize = properties.getBatchSize();

        List<OutboxEvent> stale =
                repo.findForUpdateStaleByStatus(
                        OutboxStatus.PROCESSING, staleBefore, PageRequest.of(0, batchSize));
        for (OutboxEvent e : stale) {
            log.warn(
                    "Reclaiming stale PROCESSING outbox event id={} type={} processingStartedAt={}",
                    e.getId(),
                    e.getEventType(),
                    e.getProcessingStartedAt());
            e.markProcessing();
        }

        int remaining = batchSize - stale.size();
        List<OutboxEvent> pending = List.of();
        if (remaining > 0) {
            pending =
                    repo.findForUpdateByStatus(
                            OutboxStatus.PENDING, PageRequest.of(0, remaining));
            for (OutboxEvent e : pending) {
                e.markProcessing();
            }
        }

        List<OutboxEvent> claimed = new ArrayList<>(stale.size() + pending.size());
        claimed.addAll(stale);
        claimed.addAll(pending);
        repo.saveAll(claimed);

        return claimed.stream()
                .map(e -> new ClaimedEvent(e.getId(), e.getRoutingKey(), e.getEnvelopeJson()))
                .toList();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPublished(UUID id) {
        OutboxEvent event = repo.findById(id).orElseThrow();
        event.markPublished();
        repo.save(event);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleFailure(UUID id, Throwable cause) {
        OutboxEvent event = repo.findById(id).orElse(null);
        if (event == null) {
            log.error("handleFailure called for missing outbox event id={}", id, cause);
            return;
        }
        String message = truncate(cause.getMessage());
        if (event.getAttempts() >= properties.getMaxAttempts()) {
            event.markFailed(message);
            log.error(
                    "Outbox event id={} type={} permanently FAILED after {} attempts",
                    id,
                    event.getEventType(),
                    event.getAttempts(),
                    cause);
        } else {
            event.markPendingForRetry(message);
            log.warn(
                    "Outbox event id={} type={} publish failed (attempt {}); will retry",
                    id,
                    event.getEventType(),
                    event.getAttempts(),
                    cause);
        }
        repo.save(event);
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }

    public record ClaimedEvent(UUID id, String routingKey, String envelopeJson) {}
}
