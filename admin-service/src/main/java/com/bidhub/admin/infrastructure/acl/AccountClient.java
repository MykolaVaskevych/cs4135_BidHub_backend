package com.bidhub.admin.infrastructure.acl;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * ACL client for account-service admin endpoints.
 *
 * <p>Resilience: CircuitBreaker + Retry + TimeLimiter on "account" instance (C4 evidence).
 * Service-to-service calls include X-User-Id + X-User-Roles: ADMIN headers as required by
 * account-service's HeaderAuthenticationFilter.
 */
@Component
public class AccountClient {

    private static final Logger log = LoggerFactory.getLogger(AccountClient.class);

    private final WebClient accountAdminWebClient;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final TimeLimiter timeLimiter;

    public AccountClient(
            WebClient accountAdminWebClient,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            TimeLimiterRegistry timeLimiterRegistry) {
        this.accountAdminWebClient = accountAdminWebClient;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("account");
        this.retry = retryRegistry.retry("account");
        this.timeLimiter = timeLimiterRegistry.timeLimiter("account");
    }

    /**
     * Fetches a user by ID via GET /api/admin/users/{userId}.
     * Requires adminId for X-User-Id header (forwarded from incoming request).
     */
    public Optional<UserSnapshot> fetchUser(UUID adminId, UUID userId) {
        try {
            UserSnapshot snapshot =
                    accountAdminWebClient
                            .get()
                            .uri("/api/admin/users/{userId}", userId)
                            .header("X-User-Id", adminId.toString())
                            .header("X-User-Roles", "ADMIN")
                            .retrieve()
                            .bodyToMono(UserSnapshot.class)
                            .transformDeferred(TimeLimiterOperator.of(timeLimiter))
                            .transformDeferred(RetryOperator.of(retry))
                            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                            .block();
            return Optional.ofNullable(snapshot);
        } catch (Exception ex) {
            log.warn("account-service unavailable fetching userId={}, cause={}", userId, ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Searches users via GET /api/admin/users?search=&page=&size=.
     * Returns empty list on failure (fail-open for read operations).
     */
    public List<UserSnapshot> searchUsers(UUID adminId, String keyword, int page, int size) {
        try {
            List<UserSnapshot> results =
                    accountAdminWebClient
                            .get()
                            .uri(
                                    u ->
                                            u.path("/api/admin/users")
                                                    .queryParam("search", keyword)
                                                    .queryParam("page", page)
                                                    .queryParam("size", size)
                                                    .build())
                            .header("X-User-Id", adminId.toString())
                            .header("X-User-Roles", "ADMIN")
                            .retrieve()
                            .bodyToMono(new ParameterizedTypeReference<List<UserSnapshot>>() {})
                            .transformDeferred(TimeLimiterOperator.of(timeLimiter))
                            .transformDeferred(RetryOperator.of(retry))
                            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                            .block();
            return results != null ? results : List.of();
        } catch (Exception ex) {
            log.warn("account-service unavailable searching users, cause={}", ex.getMessage());
            return List.of();
        }
    }

    /** POST /api/admin/users/{userId}/suspend */
    public Optional<UserSnapshot> suspendUser(UUID adminId, UUID userId, String reason) {
        return postAction(adminId, userId, "suspend", reason);
    }

    /** POST /api/admin/users/{userId}/ban */
    public Optional<UserSnapshot> banUser(UUID adminId, UUID userId, String reason) {
        return postAction(adminId, userId, "ban", reason);
    }

    /** POST /api/admin/users/{userId}/reactivate */
    public Optional<UserSnapshot> reactivateUser(UUID adminId, UUID userId) {
        return postAction(adminId, userId, "reactivate", null);
    }

    private Optional<UserSnapshot> postAction(
            UUID adminId, UUID userId, String action, String reason) {
        try {
            var spec =
                    accountAdminWebClient
                            .post()
                            .uri("/api/admin/users/{userId}/{action}", userId, action)
                            .header("X-User-Id", adminId.toString())
                            .header("X-User-Roles", "ADMIN");

            var bodySpec =
                    reason != null
                            ? spec.bodyValue(new ReasonBody(reason))
                            : spec.bodyValue("");

            UserSnapshot result =
                    bodySpec
                            .retrieve()
                            .bodyToMono(UserSnapshot.class)
                            .transformDeferred(TimeLimiterOperator.of(timeLimiter))
                            .transformDeferred(RetryOperator.of(retry))
                            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                            .block();
            return Optional.ofNullable(result);
        } catch (Exception ex) {
            log.warn(
                    "account-service unavailable for action={} userId={}, cause={}",
                    action, userId, ex.getMessage());
            return Optional.empty();
        }
    }

    record ReasonBody(String reason) {}
}
