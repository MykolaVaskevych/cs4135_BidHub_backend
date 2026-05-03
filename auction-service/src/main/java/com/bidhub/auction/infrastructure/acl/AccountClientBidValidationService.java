package com.bidhub.auction.infrastructure.acl;

import com.bidhub.auction.domain.exception.BidderValidationUnavailableException;
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
 * ACL implementation of {@link BidValidationService}.
 *
 * <p>Calls account-service {@code GET /api/accounts/{userId}} via load-balanced WebClient and
 * maps the {@code status} field to a {@link BidderRef}.
 *
 * <p>Fail-closed: a missing response body, transport failure, timeout, or open circuit breaker
 * is propagated as {@link BidderValidationUnavailableException} so the caller refuses the bid
 * and persists nothing. {@code SUSPENDED}, {@code BANNED}, or any non-{@code ACTIVE} status is
 * returned as {@code BidderRef.inactive}, which {@code AuctionService.placeBid} translates into
 * {@code IllegalAuctionStateException} (INV-A7).
 *
 * <p>Resilience4j {@code CircuitBreaker} + {@code Retry} + {@code TimeLimiter} are applied via
 * Reactor operators on the {@code account} instance configured in {@code auction-service-dev.yml}.
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

    @Override
    public BidderRef validateBidder(UUID bidderId) {
        AccountView view;
        try {
            view =
                    accountWebClient
                            .get()
                            .uri("/api/accounts/{userId}", bidderId)
                            .retrieve()
                            .bodyToMono(AccountView.class)
                            .transformDeferred(TimeLimiterOperator.of(timeLimiter))
                            .transformDeferred(RetryOperator.of(retry))
                            .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                            .block();
        } catch (BidderValidationUnavailableException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn(
                    "account-service unavailable for bidderId={}, refusing bid. cause={}",
                    bidderId,
                    ex.getMessage());
            throw new BidderValidationUnavailableException(
                    "Bidder validation unavailable for bidderId=" + bidderId, ex);
        }

        if (view == null) {
            throw new BidderValidationUnavailableException(
                    "Bidder validation returned no body for bidderId=" + bidderId);
        }

        return view.isEligibleToBid()
                ? BidderRef.active(bidderId)
                : BidderRef.inactive(bidderId);
    }
}
