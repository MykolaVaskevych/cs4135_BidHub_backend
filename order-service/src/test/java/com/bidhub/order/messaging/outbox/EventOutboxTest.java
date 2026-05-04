package com.bidhub.order.messaging.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bidhub.order.messaging.EventEnvelope;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventOutboxTest {

    @Mock private OutboxEventRepository repo;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @DisplayName("enqueue() persists a PENDING row whose envelopeJson decodes back to a valid EventEnvelope")
    void enqueue_persistsValidEnvelopeJson() throws Exception {
        when(repo.save(any(OutboxEvent.class))).thenAnswer(inv -> inv.getArgument(0));
        EventOutbox outbox = new EventOutbox(repo, objectMapper);

        String orderId = UUID.randomUUID().toString();
        Object payload =
                Map.of(
                        "orderId", orderId,
                        "currency", "EUR");

        OutboxEvent saved = outbox.enqueue("OrderCreated", "order.created", payload);

        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(saved.getEventType()).isEqualTo("OrderCreated");
        assertThat(saved.getRoutingKey()).isEqualTo("order.created");
        assertThat(saved.getAttempts()).isZero();
        assertThat(saved.getCreatedAt()).isNotNull();

        EventEnvelope envelope =
                objectMapper.readValue(saved.getEnvelopeJson(), EventEnvelope.class);
        assertThat(envelope.eventType()).isEqualTo("OrderCreated");
        assertThat(envelope.eventId()).isNotNull();
        assertThat(envelope.version()).isEqualTo(1);

        JsonNode body = envelope.payload();
        assertThat(body.get("orderId").asText()).isEqualTo(orderId);
        assertThat(body.get("currency").asText()).isEqualTo("EUR");
    }

    @Test
    @DisplayName("enqueue() with null payload still produces valid envelope JSON")
    void enqueue_nullPayload_doesNotThrow() throws Exception {
        when(repo.save(any(OutboxEvent.class))).thenAnswer(inv -> inv.getArgument(0));
        EventOutbox outbox = new EventOutbox(repo, objectMapper);

        OutboxEvent saved = outbox.enqueue("Heartbeat", "system.heartbeat", null);

        EventEnvelope envelope =
                objectMapper.readValue(saved.getEnvelopeJson(), EventEnvelope.class);
        assertThat(envelope.eventType()).isEqualTo("Heartbeat");
        assertThat(envelope.payload().isNull()).isTrue();
    }
}
