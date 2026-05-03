package com.bidhub.auction.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bidhub.auction.domain.exception.BidderValidationUnavailableException;
import com.bidhub.auction.infrastructure.acl.AccountClientBidValidationService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

/**
 * Verifies that AccountClientBidValidationService fails closed when account-service is
 * unreachable: validateBidder() must propagate {@link BidderValidationUnavailableException}
 * so the bid is rejected (HTTP 503) and no row is persisted, rather than silently allowing
 * the bid through.
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
    @DisplayName("Fail-closed: validateBidder throws when account-service is unreachable")
    void validateBidder_accountServiceDown_throwsUnavailable() {
        UUID bidderId = UUID.randomUUID();

        assertThatThrownBy(() -> bidValidationService.validateBidder(bidderId))
                .isInstanceOf(BidderValidationUnavailableException.class);
    }
}
