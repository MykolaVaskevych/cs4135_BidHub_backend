package com.bidhub.auction.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EventEnvelope(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        int version,
        JsonNode payload) {

    public static EventEnvelope newEvent(String eventType, JsonNode payload) {
        return new EventEnvelope(UUID.randomUUID(), eventType, Instant.now(), 1, payload);
    }
}
