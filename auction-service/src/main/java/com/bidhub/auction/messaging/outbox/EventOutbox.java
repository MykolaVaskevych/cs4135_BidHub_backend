package com.bidhub.auction.messaging.outbox;

import com.bidhub.auction.messaging.EventEnvelope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class EventOutbox {

    private final OutboxEventRepository repo;
    private final ObjectMapper objectMapper;

    public EventOutbox(OutboxEventRepository repo, ObjectMapper objectMapper) {
        this.repo = repo;
        this.objectMapper = objectMapper;
    }

    public OutboxEvent enqueue(String eventType, String routingKey, Object payload) {
        try {
            JsonNode payloadNode = objectMapper.valueToTree(payload);
            EventEnvelope envelope = EventEnvelope.newEvent(eventType, payloadNode);
            String envelopeJson = objectMapper.writeValueAsString(envelope);
            return repo.save(OutboxEvent.pending(eventType, routingKey, envelopeJson));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize event payload for " + eventType, e);
        }
    }
}
