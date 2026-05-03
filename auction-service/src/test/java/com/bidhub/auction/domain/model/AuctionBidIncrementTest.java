package com.bidhub.auction.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bidhub.auction.domain.exception.BidTooLowException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Domain-level coverage for the B7 minimum-increment rule:
 * the first bid must reach the starting price, and every subsequent bid must clear
 * the current price by at least €1.
 */
class AuctionBidIncrementTest {

    private static final UUID SELLER = UUID.randomUUID();
    private static final UUID BIDDER_A = UUID.randomUUID();
    private static final UUID BIDDER_B = UUID.randomUUID();

    private Auction freshAuction(double startingPrice) {
        return Auction.create(
                UUID.randomUUID(),
                SELLER,
                Money.of(BigDecimal.valueOf(startingPrice)),
                Money.of(BigDecimal.valueOf(startingPrice + 5)),
                null,
                AuctionDuration.of(Instant.now(), Instant.now().plusSeconds(3600)));
    }

    @Test
    @DisplayName("First bid equal to startingPrice is accepted (no €1 increment required)")
    void firstBid_atStartingPrice_isAccepted() {
        Auction auction = freshAuction(10.00);

        Bid bid = auction.placeBid(BIDDER_A, Money.of(BigDecimal.valueOf(10.00)));

        assertThat(bid.getAmount().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(10.00));
        assertThat(auction.getCurrentPrice().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(10.00));
    }

    @Test
    @DisplayName("First bid below startingPrice is rejected")
    void firstBid_belowStartingPrice_throws() {
        Auction auction = freshAuction(10.00);

        assertThatThrownBy(
                        () ->
                                auction.placeBid(
                                        BIDDER_A, Money.of(BigDecimal.valueOf(9.99))))
                .isInstanceOf(BidTooLowException.class);
    }

    @Test
    @DisplayName("Second bid only €0.99 above current price is rejected (B7 €1 rule)")
    void secondBid_under1EuroIncrement_throws() {
        Auction auction = freshAuction(10.00);
        auction.placeBid(BIDDER_A, Money.of(BigDecimal.valueOf(10.00)));

        assertThatThrownBy(
                        () ->
                                auction.placeBid(
                                        BIDDER_B, Money.of(BigDecimal.valueOf(10.99))))
                .isInstanceOf(BidTooLowException.class);
    }

    @Test
    @DisplayName("Second bid equal to current price is rejected (must clear by €1)")
    void secondBid_equalToCurrentPrice_throws() {
        Auction auction = freshAuction(10.00);
        auction.placeBid(BIDDER_A, Money.of(BigDecimal.valueOf(10.00)));

        assertThatThrownBy(
                        () ->
                                auction.placeBid(
                                        BIDDER_B, Money.of(BigDecimal.valueOf(10.00))))
                .isInstanceOf(BidTooLowException.class);
    }

    @Test
    @DisplayName("Second bid exactly €1 above current price is accepted")
    void secondBid_exactly1EuroAbove_isAccepted() {
        Auction auction = freshAuction(10.00);
        auction.placeBid(BIDDER_A, Money.of(BigDecimal.valueOf(10.00)));

        Bid bid = auction.placeBid(BIDDER_B, Money.of(BigDecimal.valueOf(11.00)));

        assertThat(bid.getAmount().getAmount()).isEqualByComparingTo(BigDecimal.valueOf(11.00));
    }
}
