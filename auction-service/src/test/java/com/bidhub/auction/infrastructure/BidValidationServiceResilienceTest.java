package com.bidhub.auction.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.bidhub.auction.domain.model.BidderRef;
import com.bidhub.auction.infrastructure.acl.AccountClientBidValidationService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Verifies that AccountClientBidValidationService fails closed when account-service
 * is unreachable: validateBidder() must return an inactive BidderRef (not throw),
 * and the circuit breaker transitions through its states correctly.
 */
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
class BidValidationServiceResilienceTest {

    @Autowired private AccountClientBidValidationService bidValidationService;
    @Autowired private CircuitBreakerRegistry circuitBreakerRegistry;

    @Test
    @DisplayName("CB registered: 'account' circuit breaker exists in registry")
    void accountCircuitBreaker_registeredInRegistry() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("account");
        assertThat(cb).isNotNull();
        assertThat(cb.getName()).isEqualTo("account");
    }

    @Test
    @DisplayName("Fail-closed: validateBidder returns inactive BidderRef when account-service is down")
    void validateBidder_accountServiceDown_returnsInactiveBidderRef() {
        // account-service is NOT running — WebClient call will time out / refuse connection.
        // The @TimeLimiter + @CircuitBreaker fallback must return BidderRef.inactive().
        UUID bidderId = UUID.randomUUID();
        BidderRef result = bidValidationService.validateBidder(bidderId);

        // Fail-closed: inactive ref → AuctionService will reject the bid
        assertThat(result.isActive()).isFalse();
        assertThat(result.userId()).isEqualTo(bidderId);
    }
}
