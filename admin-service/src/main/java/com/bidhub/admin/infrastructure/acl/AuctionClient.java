package com.bidhub.admin.infrastructure.acl;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.core.ParameterizedTypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * ACL client for auction-service.
 *
 * <p>Resilience: CircuitBreaker + TimeLimiter on "auction" instance (C4 evidence).
 * Fail-open for read operations — returns empty on failure.
 */
@Component
public class AuctionClient {

    private static final Logger log = LoggerFactory.getLogger(AuctionClient.class);

    private final WebClient auctionWebClient;
    private final CircuitBreaker circuitBreaker;
    private final TimeLimiter timeLimiter;

    public AuctionClient(
            WebClient auctionWebClient,
            CircuitBreakerRegistry circuitBreakerRegistry,
            TimeLimiterRegistry timeLimiterRegistry) {
        this.auctionWebClient = auctionWebClient;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("auction");
        this.timeLimiter = timeLimiterRegistry.timeLimiter("auction");
    }

    /**
     * Fetches listing details from auction-service.
     * Fail-open: returns empty Optional when auction-service is unreachable.
     */
    public Optional<ListingSnapshot> fetchListing(UUID listingId) {
        try {
            ListingSnapshot snapshot =
                    auctionWebClient
                            .get()
                            .uri("/api/auctions/listings/{listingId}", listingId)
                            .retrieve()
                            .bodyToMono(ListingSnapshot.class)
                            .transformDeferred(TimeLimiterOperator.of(timeLimiter))
                            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                            .block();
            return Optional.ofNullable(snapshot);
        } catch (Exception ex) {
            log.warn("auction-service unavailable fetching listingId={}, cause={}", listingId, ex.getMessage());
            return Optional.empty();
        }
    }

    /** Returns count of ACTIVE auctions. Returns -1 on failure. */
    public long countActiveAuctions() {
        try {
            List<AuctionSnapshot> all =
                    auctionWebClient
                            .get()
                            .uri("/api/auctions/all")
                            .retrieve()
                            .bodyToMono(new ParameterizedTypeReference<List<AuctionSnapshot>>() {})
                            .transformDeferred(TimeLimiterOperator.of(timeLimiter))
                            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                            .block();
            if (all == null) return 0;
            return all.stream().filter(a -> "ACTIVE".equals(a.status())).count();
        } catch (Exception ex) {
            log.warn("auction-service unavailable for countActiveAuctions, cause={}", ex.getMessage());
            return -1;
        }
    }

    /** Value object returned by auction-service listing endpoint. */
    public record ListingSnapshot(
            UUID listingId,
            String title,
            String status,
            UUID sellerId) {}

    /** Minimal view of an auction for count purposes. */
    public record AuctionSnapshot(UUID auctionId, String status) {}
}
