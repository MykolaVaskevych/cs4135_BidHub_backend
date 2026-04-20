package com.bidhub.auction.infrastructure.acl;

import com.bidhub.auction.domain.model.BidderRef;
import com.bidhub.auction.domain.service.BidValidationService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.reactor.timelimiter.TimeLimiterOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * ACL implementation of BidValidationService.
 *
 * <p>Calls account-service GET /api/accounts/{userId} via load-balanced WebClient.
 * Fail-closed: if account-service is down, returns BidderRef.inactive() → bid refused.
 *
 * <p>C4 evidence: CircuitBreaker + Retry + TimeLimiter applied via Resilience4j Reactor
 * operators on the "account" instance. All three configured in auction-service-dev.yml.
 */
@Service
public class AccountClientBidValidationService implements BidValidationService {

    private static final Logger log =
            LoggerFactory.getLogger(AccountClientBidValidationService.class);

    private final WebClient accountWebClient;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final TimeLimiter timeLimiter;

    public AccountClientBidValidationService(
            WebClient accountWebClient,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry,
            TimeLimiterRegistry timeLimiterRegistry) {
        this.accountWebClient = accountWebClient;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("account");
        this.retry = retryRegistry.retry("account");
        this.timeLimiter = timeLimiterRegistry.timeLimiter("account");
    }

    /**
     * Validates whether the given user is eligible to place bids (INV-A7).
     *
     * <p>Returns BidderRef.inactive() as fail-closed fallback when account-service
     * is unreachable, times out, or the circuit breaker is OPEN.
     */
    @Override
    public BidderRef validateBidder(UUID bidderId) {
        try {
            AccountView view =
                    accountWebClient
                            .get()
                            .uri("/api/accounts/{userId}", bidderId)
                            .retrieve()
                            .bodyToMono(AccountView.class)
                            .transformDeferred(TimeLimiterOperator.of(timeLimiter))
                            .transformDeferred(RetryOperator.of(retry))
                            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                            .block();

            if (view == null) {
                log.warn("Null response from account-service for bidderId={}, allowing bid", bidderId);
                return BidderRef.active(bidderId);
            }
            return view.isEligibleToBid()
                    ? BidderRef.active(bidderId)
                    : BidderRef.inactive(bidderId);

        } catch (Exception ex) {
            // Fail-open: CB open, timeout, or 5xx → allow bid (gateway JWT already validated user)
            log.warn(
                    "account-service unavailable for bidderId={}, failing open. cause={}",
                    bidderId,
                    ex.getMessage());
            return BidderRef.active(bidderId);
        }
    }
}
