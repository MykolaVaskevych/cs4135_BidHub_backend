package com.bidhub.auction.infrastructure.acl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.bidhub.auction.domain.exception.BidderValidationUnavailableException;
import com.bidhub.auction.domain.model.BidderRef;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class AccountClientBidValidationServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private WebClient accountWebClient;

    private AccountClientBidValidationService service;

    private static final UUID BIDDER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service =
                new AccountClientBidValidationService(
                        accountWebClient,
                        CircuitBreakerRegistry.ofDefaults(),
                        RetryRegistry.ofDefaults(),
                        TimeLimiterRegistry.ofDefaults());
    }

    @Test
    @DisplayName("ACTIVE bidder returns BidderRef.active")
    void activeBidder_returnsActive() {
        stubBodyToMono(Mono.just(new AccountView(BIDDER_ID, "ACTIVE")));

        BidderRef result = service.validateBidder(BIDDER_ID);

        assertThat(result.isActive()).isTrue();
    }

    @Test
    @DisplayName("SUSPENDED / BANNED / non-ACTIVE bidder returns BidderRef.inactive")
    void inactiveBidder_returnsInactive() {
        stubBodyToMono(Mono.just(new AccountView(BIDDER_ID, "SUSPENDED")));

        BidderRef result = service.validateBidder(BIDDER_ID);

        assertThat(result.isActive()).isFalse();
    }

    @Test
    @DisplayName("Empty body throws BidderValidationUnavailableException — no fail-open to active")
    void nullResponse_throwsUnavailable() {
        stubBodyToMono(Mono.empty());

        assertThatThrownBy(() -> service.validateBidder(BIDDER_ID))
                .isInstanceOf(BidderValidationUnavailableException.class);
    }

    @Test
    @DisplayName("Transport / 5xx / timeout throws BidderValidationUnavailableException — no fail-open")
    void transportError_throwsUnavailable() {
        stubBodyToMono(Mono.error(new RuntimeException("connection refused")));

        assertThatThrownBy(() -> service.validateBidder(BIDDER_ID))
                .isInstanceOf(BidderValidationUnavailableException.class);
    }

    private void stubBodyToMono(Mono<AccountView> response) {
        when(accountWebClient
                        .get()
                        .uri(anyString(), any(UUID.class))
                        .retrieve()
                        .bodyToMono(AccountView.class))
                .thenReturn(response);
    }
}
