package com.bidhub.auction.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EventEnvelopeJsonRoundTripTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    @DisplayName("Envelope serializes to JSON and deserializes back to identical fields")
    void roundTrip_preservesAllFields() throws Exception {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("auctionId", UUID.randomUUID().toString());
        payload.put("currency", "EUR");

        EventEnvelope original =
                new EventEnvelope(
                        UUID.randomUUID(),
                        "AuctionSold",
                        Instant.parse("2026-05-04T10:00:00Z"),
                        1,
                        payload);

        String json = objectMapper.writeValueAsString(original);
        EventEnvelope decoded = objectMapper.readValue(json, EventEnvelope.class);

        assertThat(decoded.eventId()).isEqualTo(original.eventId());
        assertThat(decoded.eventType()).isEqualTo("AuctionSold");
        assertThat(decoded.occurredAt()).isEqualTo(Instant.parse("2026-05-04T10:00:00Z"));
        assertThat(decoded.version()).isEqualTo(1);
        assertThat(decoded.payload().get("auctionId").asText())
                .isEqualTo(payload.get("auctionId").asText());
        assertThat(decoded.payload().get("currency").asText()).isEqualTo("EUR");
    }

    @Test
    @DisplayName("newEvent() factory generates random eventId and current occurredAt")
    void newEvent_generatesIdAndTimestamp() {
        ObjectNode payload = objectMapper.createObjectNode();
        Instant before = Instant.now();

        EventEnvelope e1 = EventEnvelope.newEvent("X", payload);
        EventEnvelope e2 = EventEnvelope.newEvent("X", payload);

        assertThat(e1.eventId()).isNotNull().isNotEqualTo(e2.eventId());
        assertThat(e1.occurredAt()).isAfterOrEqualTo(before);
        assertThat(e1.version()).isEqualTo(1);
    }

    @Test
    @DisplayName("Unknown JSON properties are ignored on deserialize")
    void deserialize_ignoresUnknownProperties() throws Exception {
        String json =
                """
                {
                  "eventId": "00000000-0000-0000-0000-000000000001",
                  "eventType": "Whatever",
                  "occurredAt": "2026-05-04T10:00:00Z",
                  "version": 1,
                  "payload": {"x": 1},
                  "futureField": "tolerated"
                }
                """;

        EventEnvelope decoded = objectMapper.readValue(json, EventEnvelope.class);

        assertThat(decoded.eventType()).isEqualTo("Whatever");
        JsonNode payload = decoded.payload();
        assertThat(payload.get("x").asInt()).isEqualTo(1);
    }
}
