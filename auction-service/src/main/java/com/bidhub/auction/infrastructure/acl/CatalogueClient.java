package com.bidhub.auction.infrastructure.acl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.scheduler.Schedulers;

/**
 * ACL: pushes auction lifecycle events to catalogue-service as REST calls.
 * Temporary REST projection update until catalogue event consumers are implemented.
 * Fire-and-forget — errors are logged, never thrown.
 */
@Component
public class CatalogueClient {

    private static final Logger log = LoggerFactory.getLogger(CatalogueClient.class);

    private final WebClient catalogueWebClient;

    public CatalogueClient(WebClient catalogueWebClient) {
        this.catalogueWebClient = catalogueWebClient;
    }

    /** Index a new listing when its auction is created. */
    public void indexListingAsync(UUID listingId, String title, UUID categoryId,
                                  UUID sellerId, BigDecimal startingPrice, Instant endTime) {
        Map<String, Object> body = Map.of(
                "listingId", listingId,
                "title", title,
                "categoryId", categoryId,
                "sellerId", sellerId,
                "startingPrice", startingPrice,
                "endTime", endTime);
        catalogueWebClient.post()
                .uri("/api/catalogue/internal/listings")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        r -> log.info("Listing {} indexed in catalogue", listingId),
                        e -> log.warn("Failed to index listing {} in catalogue: {}",
                                listingId, e.getMessage()));
    }

    /** Update the current price after a bid is placed. */
    public void updatePriceAsync(UUID listingId, BigDecimal newPrice) {
        catalogueWebClient.put()
                .uri("/api/catalogue/internal/listings/{id}/price", listingId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("newPrice", newPrice))
                .retrieve()
                .toBodilessEntity()
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        r -> log.debug("Price updated for listing {} in catalogue", listingId),
                        e -> log.warn("Failed to update price for listing {} in catalogue: {}",
                                listingId, e.getMessage()));
    }

    /** Update listing status (SOLD, ENDED, CANCELLED, REMOVED). */
    public void updateStatusAsync(UUID listingId, String status) {
        catalogueWebClient.put()
                .uri("/api/catalogue/internal/listings/{id}/status", listingId)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("status", status))
                .retrieve()
                .toBodilessEntity()
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        r -> log.info("Status {} set for listing {} in catalogue", status, listingId),
                        e -> log.warn("Failed to update status for listing {} in catalogue: {}",
                                listingId, e.getMessage()));
    }
}
