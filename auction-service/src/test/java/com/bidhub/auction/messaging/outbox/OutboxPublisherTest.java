package com.bidhub.auction.messaging.outbox;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bidhub.auction.messaging.RabbitConfig;
import com.bidhub.auction.messaging.outbox.OutboxEventStateService.ClaimedEvent;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock private OutboxEventStateService stateService;
    @Mock private RabbitTemplate rabbitTemplate;
    @InjectMocks private OutboxPublisher publisher;

    @Test
    @DisplayName("publishPending: each claimed event is sent and marked PUBLISHED via state service")
    void publishPending_success_marksPublishedViaStateService() {
        ClaimedEvent c = new ClaimedEvent(UUID.randomUUID(), "x.routing", "{\"a\":1}");
        when(stateService.claimBatch()).thenReturn(List.of(c));

        publisher.publishPending();

        verify(rabbitTemplate).send(eq(RabbitConfig.EXCHANGE), eq("x.routing"), any(Message.class));
        verify(stateService).markPublished(c.id());
    }

    @Test
    @DisplayName("publishPending: broker error delegates to state service handleFailure")
    void publishPending_brokerError_delegatesHandleFailure() {
        ClaimedEvent c = new ClaimedEvent(UUID.randomUUID(), "bad.routing", "{}");
        when(stateService.claimBatch()).thenReturn(List.of(c));
        doThrow(new AmqpException("broker down"))
                .when(rabbitTemplate)
                .send(anyString(), eq("bad.routing"), any(Message.class));

        publisher.publishPending();

        verify(stateService).handleFailure(eq(c.id()), any(AmqpException.class));
    }

    @Test
    @DisplayName("publishPending: empty claim batch is a no-op (no broker calls)")
    void publishPending_emptyClaim_noBrokerCalls() {
        when(stateService.claimBatch()).thenReturn(List.of());

        publisher.publishPending();

        verify(rabbitTemplate, org.mockito.Mockito.never())
                .send(anyString(), anyString(), any(Message.class));
    }
}
