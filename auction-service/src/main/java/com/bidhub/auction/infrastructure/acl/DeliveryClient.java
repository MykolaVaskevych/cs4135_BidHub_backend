package com.bidhub.auction.infrastructure.acl;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * ACL: fires a delivery job creation request to delivery-service after a sale.
 * Best-effort — failure is logged but does not roll back the auction sale.
 */
@Component
public class DeliveryClient {

    private static final Logger log = LoggerFactory.getLogger(DeliveryClient.class);

    private static final Map<String, String> PLACEHOLDER_ADDRESS =
            Map.of("street", "TBD", "city", "TBD", "county", "TBD", "eircode", "TBD");

    private final WebClient deliveryWebClient;

    public DeliveryClient(WebClient deliveryWebClient) {
        this.deliveryWebClient = deliveryWebClient;
    }

    public void createJobAsync(UUID auctionId, UUID sellerId, UUID buyerId, BigDecimal amount) {
        Map<String, Object> body = Map.of(
                "orderId", auctionId,
                "auctionId", auctionId,
                "sellerId", sellerId,
                "buyerId", buyerId,
                "pickupAddress", PLACEHOLDER_ADDRESS,
                "deliveryAddress", PLACEHOLDER_ADDRESS,
                "escrowId", UUID.randomUUID(),
                "escrowAmount", amount
        );

        try {
            deliveryWebClient.post()
                    .uri("/api/delivery")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            log.info("Delivery job created for auctionId={}", auctionId);
        } catch (Exception e) {
            log.warn("Failed to create delivery job for auctionId={}: {}", auctionId, e.getMessage());
        }
    }
}
