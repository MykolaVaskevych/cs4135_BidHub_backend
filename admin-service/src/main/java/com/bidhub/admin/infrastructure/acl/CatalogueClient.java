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
 * <p>BLOCKED: GET /api/catalogue/categories/{id}/active-count not yet implemented by Zihan.
 * Strict fail-closed fallback: returns -1L (unknown count) → deactivation refused.
 *
 * <p>Resilience: CircuitBreaker + TimeLimiter on "catalogue" instance (C4 evidence).
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
     * Fail-closed: returns -1L when catalogue-service is unreachable.
     * -1 → CategoryManagementService treats as "unknown, refuse deactivation" (INV-2).
     *
     * <p>BLOCKED: endpoint not yet available — this method always hits the fallback.
     */
    public long countActiveListings(UUID categoryId) {
        try {
            Long count =
                    catalogueWebClient
                            .get()
                            .uri("/api/catalogue/categories/{categoryId}/active-count", categoryId)
                            .retrieve()
                            .bodyToMono(Long.class)
                            .transformDeferred(TimeLimiterOperator.of(timeLimiter))
                            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                            .block();
            return count != null ? count : 0L;
        } catch (Exception ex) {
            log.warn(
                    "catalogue-service unavailable for categoryId={}, failing closed (INV-2). cause={}",
                    categoryId, ex.getMessage());
            return -1L; // unknown — caller must refuse deactivation
        }
    }
}
