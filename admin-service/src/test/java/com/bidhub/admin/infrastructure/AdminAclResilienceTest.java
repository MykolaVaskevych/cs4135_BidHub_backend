package com.bidhub.admin.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.bidhub.admin.infrastructure.acl.AccountClient;
import com.bidhub.admin.infrastructure.acl.UserSnapshot;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

/**
 * Verifies that admin-service ACL clients fail gracefully when upstream services are down.
 * Runs against the real Spring context (no mocks), with services not running.
 */
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
class AdminAclResilienceTest {

    @Autowired private AccountClient accountClient;
    @Autowired private CircuitBreakerRegistry circuitBreakerRegistry;

    @Test
    @DisplayName("'account' CB registered in admin-service registry")
    void accountCircuitBreaker_registeredInRegistry() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("account");
        assertThat(cb).isNotNull();
        assertThat(cb.getName()).isEqualTo("account");
    }

    @Test
    @DisplayName("'auction' CB registered in admin-service registry")
    void auctionCircuitBreaker_registeredInRegistry() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("auction");
        assertThat(cb).isNotNull();
    }

    @Test
    @DisplayName("'catalogue' CB registered in admin-service registry")
    void catalogueCircuitBreaker_registeredInRegistry() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("catalogue");
        assertThat(cb).isNotNull();
    }

    @Test
    @DisplayName("Fail-open: fetchUser returns empty Optional when account-service is down")
    void fetchUser_accountServiceDown_returnsEmpty() {
        // account-service not running — CB fallback must return empty, not throw
        Optional<UserSnapshot> result = accountClient.fetchUser(UUID.randomUUID(), UUID.randomUUID());
        assertThat(result).isEmpty();
    }
}
