package com.bidhub.auction.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.bidhub.auction.domain.model.AuctionDuration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuctionDurationTest {

    @Test
    @DisplayName("isExpired returns true when endTime is in the past")
    void isExpired_past() {
        AuctionDuration duration =
                AuctionDuration.of(
                        Instant.now().minusSeconds(3600), Instant.now().minusSeconds(1));
        assertThat(duration.isExpired()).isTrue();
    }

    @Test
    @DisplayName("isExpired returns false when endTime is in the future")
    void isExpired_future() {
        AuctionDuration duration =
                AuctionDuration.of(Instant.now(), Instant.now().plusSeconds(3600));
        assertThat(duration.isExpired()).isFalse();
    }

    @Test
    @DisplayName("remainingSeconds is positive when auction is still active")
    void remainingSeconds_future() {
        AuctionDuration duration =
                AuctionDuration.of(Instant.now(), Instant.now().plusSeconds(3600));
        assertThat(duration.remainingSeconds()).isPositive();
    }

    @Test
    @DisplayName("remainingSeconds is zero or negative when auction has ended")
    void remainingSeconds_expired() {
        AuctionDuration duration =
                AuctionDuration.of(
                        Instant.now().minusSeconds(3600), Instant.now().minusSeconds(1));
        assertThat(duration.remainingSeconds()).isNotPositive();
    }
}
