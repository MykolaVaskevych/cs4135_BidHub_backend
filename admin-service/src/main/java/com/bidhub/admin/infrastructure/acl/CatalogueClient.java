package com.bidhub.admin.infrastructure.acl;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * ACL client for catalogue-service.
 *
 * <p>Resilience: CircuitBreaker + TimeLimiter on "catalogue" instance (C4 evidence).
 * Fail-closed: returns -1L when catalogue-service is unreachable → deactivation refused (INV-2).
 */
@Component
public class CatalogueClient {

    private static final Logger log = LoggerFactory.getLogger(CatalogueClient.class);

    private final WebClient catalogueWebClient;
    private final CircuitBreaker circuitBreaker;
    private final TimeLimiter timeLimiter;

    public CatalogueClient(
            WebClient catalogueWebClient,
            CircuitBreakerRegistry circuitBreakerRegistry,
            TimeLimiterRegistry timeLimiterRegistry) {
        this.catalogueWebClient = catalogueWebClient;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("catalogue");
        this.timeLimiter = timeLimiterRegistry.timeLimiter("catalogue");
    }

    /**
     * Returns the number of active listings in a category.
     * Catalogue returns {"activeCount": N} — we extract the value.
     * Fail-closed: returns -1L when catalogue-service is unreachable (INV-2).
     */
    public long countActiveListings(UUID categoryId) {
        try {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Long> body =
                    (java.util.Map<String, Long>) catalogueWebClient
                            .get()
                            .uri("/api/catalogue/categories/{categoryId}/active-count", categoryId)
                            .retrieve()
                            .bodyToMono(java.util.Map.class)
                            .transformDeferred(TimeLimiterOperator.of(timeLimiter))
                            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                            .block();
            if (body == null) return 0L;
            Number count = (Number) body.get("activeCount");
            return count != null ? count.longValue() : 0L;
        } catch (Exception ex) {
            log.warn(
                    "catalogue-service unavailable for categoryId={}, failing closed (INV-2). cause={}",
                    categoryId, ex.getMessage());
            return -1L;
        }
    }
}
