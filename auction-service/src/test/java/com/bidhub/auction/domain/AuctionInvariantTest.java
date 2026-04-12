package com.bidhub.auction.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bidhub.auction.domain.exception.BidTooLowException;
import com.bidhub.auction.domain.exception.IllegalAuctionStateException;
import com.bidhub.auction.domain.exception.SellerBidException;
import com.bidhub.auction.domain.model.Auction;
import com.bidhub.auction.domain.model.AuctionDuration;
import com.bidhub.auction.domain.model.AuctionStatus;
import com.bidhub.auction.domain.model.Bid;
import com.bidhub.auction.domain.model.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuctionInvariantTest {

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID BUYER_1 = UUID.randomUUID();
    private static final UUID BUYER_2 = UUID.randomUUID();

    /** Returns a fresh ACTIVE auction: starting price 10, reserve 20, no buy-now, 1h duration. */
    private Auction activeAuction() {
        return Auction.create(
                UUID.randomUUID(),
                SELLER_ID,
                Money.of(BigDecimal.valueOf(10)),
                Money.of(BigDecimal.valueOf(20)),
                null,
                AuctionDuration.of(Instant.now(), Instant.now().plusSeconds(3600)));
    }

    /** Returns an ACTIVE auction with buy-now price set. */
    private Auction auctionWithBuyNow(BigDecimal buyNowPrice) {
        return Auction.create(
                UUID.randomUUID(),
                SELLER_ID,
                Money.of(BigDecimal.valueOf(10)),
                Money.of(BigDecimal.valueOf(20)),
                Money.of(buyNowPrice),
                AuctionDuration.of(Instant.now(), Instant.now().plusSeconds(3600)));
    }

    // ---------------------------------------------------------------
    // INV-A1: bid.amount must be strictly greater than currentPrice
    // ---------------------------------------------------------------

    @Test
    @DisplayName("INV-A1: bid equal to currentPrice is rejected")
    void invA1_bidEqualToCurrentPrice_rejected() {
        Auction auction = activeAuction(); // currentPrice = startingPrice = 10
        assertThatThrownBy(() -> auction.placeBid(BUYER_1, Money.of(BigDecimal.valueOf(10))))
                .isInstanceOf(BidTooLowException.class);
    }

    @Test
    @DisplayName("INV-A1: bid below currentPrice is rejected")
    void invA1_bidBelowCurrentPrice_rejected() {
        Auction auction = activeAuction();
        assertThatThrownBy(() -> auction.placeBid(BUYER_1, Money.of(BigDecimal.valueOf(5))))
                .isInstanceOf(BidTooLowException.class);
    }

    @Test
    @DisplayName("INV-A1: bid strictly above currentPrice is accepted")
    void invA1_bidAboveCurrentPrice_accepted() {
        Auction auction = activeAuction();
        Bid bid = auction.placeBid(BUYER_1, Money.of(BigDecimal.valueOf(11)));
        assertThat(bid).isNotNull();
    }

    // ---------------------------------------------------------------
    // INV-A2: bids only accepted when status = ACTIVE
    // ---------------------------------------------------------------

    @Test
    @DisplayName("INV-A2: bid rejected on CANCELLED auction")
    void invA2_bidOnCancelledAuction_rejected() {
        Auction auction = activeAuction();
        auction.cancel(); // no bids yet → valid cancel
        assertThatThrownBy(() -> auction.placeBid(BUYER_1, Money.of(BigDecimal.valueOf(11))))
                .isInstanceOf(IllegalAuctionStateException.class);
    }

    @Test
    @DisplayName("INV-A2: bid rejected on ENDED auction")
    void invA2_bidOnEndedAuction_rejected() {
        Auction auction = activeAuction();
        auction.close(); // no bids → ENDED
        assertThatThrownBy(() -> auction.placeBid(BUYER_1, Money.of(BigDecimal.valueOf(11))))
                .isInstanceOf(IllegalAuctionStateException.class);
    }

    // ---------------------------------------------------------------
    // INV-A3: buy-now only when status=ACTIVE, buyNowPrice set, buyer≠seller
    // ---------------------------------------------------------------

    @Test
    @DisplayName("INV-A3: buy-now rejected when buyNowPrice is not set")
    void invA3_buyNowWithoutPrice_rejected() {
        Auction auction = activeAuction(); // no buy-now price
        assertThatThrownBy(() -> auction.buyNow(BUYER_1))
                .isInstanceOf(IllegalAuctionStateException.class);
    }

    @Test
    @DisplayName("INV-A3: buy-now rejected when buyer is the seller")
    void invA3_buyNowBySeller_rejected() {
        Auction auction = auctionWithBuyNow(BigDecimal.valueOf(50));
        assertThatThrownBy(() -> auction.buyNow(SELLER_ID))
                .isInstanceOf(SellerBidException.class);
    }

    @Test
    @DisplayName("INV-A3: buy-now rejected when auction is not ACTIVE")
    void invA3_buyNowOnNonActiveAuction_rejected() {
        Auction auction = auctionWithBuyNow(BigDecimal.valueOf(50));
        auction.cancel();
        assertThatThrownBy(() -> auction.buyNow(BUYER_1))
                .isInstanceOf(IllegalAuctionStateException.class);
    }

    @Test
    @DisplayName("INV-A3: buy-now succeeds with price set, ACTIVE, buyer≠seller")
    void invA3_buyNow_succeeds() {
        Auction auction = auctionWithBuyNow(BigDecimal.valueOf(50));
        auction.buyNow(BUYER_1);
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.SOLD);
    }

    // ---------------------------------------------------------------
    // INV-A4: cancel only when ACTIVE and bidCount = 0
    // ---------------------------------------------------------------

    @Test
    @DisplayName("INV-A4: cancel rejected when bids exist")
    void invA4_cancelWithBids_rejected() {
        Auction auction = activeAuction();
        auction.placeBid(BUYER_1, Money.of(BigDecimal.valueOf(11)));
        assertThatThrownBy(() -> auction.cancel())
                .isInstanceOf(IllegalAuctionStateException.class);
    }

    @Test
    @DisplayName("INV-A4: cancel succeeds when ACTIVE and no bids")
    void invA4_cancelNoBids_succeeds() {
        Auction auction = activeAuction();
        auction.cancel();
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.CANCELLED);
    }

    // ---------------------------------------------------------------
    // INV-A5: close → SOLD if highestBid >= reservePrice, else ENDED
    // ---------------------------------------------------------------

    @Test
    @DisplayName("INV-A5: close with bid meeting reserve → SOLD")
    void invA5_closeWithQualifyingBid_sold() {
        Auction auction = activeAuction(); // reserve = 20
        auction.placeBid(BUYER_1, Money.of(BigDecimal.valueOf(20)));
        auction.close();
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.SOLD);
    }

    @Test
    @DisplayName("INV-A5: close with bid below reserve → ENDED")
    void invA5_closeWithBidBelowReserve_ended() {
        Auction auction = activeAuction(); // reserve = 20, starting = 10
        auction.placeBid(BUYER_1, Money.of(BigDecimal.valueOf(15))); // 15 < 20
        auction.close();
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.ENDED);
    }

    @Test
    @DisplayName("INV-A5: close with no bids → ENDED")
    void invA5_closeNoBids_ended() {
        Auction auction = activeAuction();
        auction.close();
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.ENDED);
    }

    // ---------------------------------------------------------------
    // INV-A6: @Version field exists for optimistic locking
    // ---------------------------------------------------------------

    @Test
    @DisplayName("INV-A6: Auction has a version field for optimistic locking")
    void invA6_versionFieldExists() {
        Auction auction = activeAuction();
        // Version starts at null before first persistence save.
        // We just verify the accessor exists and doesn't throw.
        // Real OCC behaviour is proven in integration tests (Phase 5).
        auction.getVersion(); // must not throw
    }

    // ---------------------------------------------------------------
    // INV-A8: seller cannot bid on own auction
    // ---------------------------------------------------------------

    @Test
    @DisplayName("INV-A8: seller bidding on own auction is rejected")
    void invA8_sellerBidsOnOwnAuction_rejected() {
        Auction auction = activeAuction();
        assertThatThrownBy(() -> auction.placeBid(SELLER_ID, Money.of(BigDecimal.valueOf(11))))
                .isInstanceOf(SellerBidException.class);
    }

    @Test
    @DisplayName("INV-A8: different buyer can bid successfully")
    void invA8_differentBuyer_accepted() {
        Auction auction = activeAuction();
        Bid bid = auction.placeBid(BUYER_1, Money.of(BigDecimal.valueOf(11)));
        assertThat(bid.getBidderId()).isEqualTo(BUYER_1);
    }

    // ---------------------------------------------------------------
    // INV-A9: currentPrice = max bid amount, or startingPrice if no bids
    // ---------------------------------------------------------------

    @Test
    @DisplayName("INV-A9: currentPrice equals startingPrice before any bids")
    void invA9_currentPriceInitiallyStartingPrice() {
        Auction auction = activeAuction(); // startingPrice = 10
        assertThat(auction.getCurrentPrice().getAmount().compareTo(BigDecimal.valueOf(10)))
                .isZero();
    }

    @Test
    @DisplayName("INV-A9: currentPrice updates to highest bid after each successful bid")
    void invA9_currentPriceUpdatesOnBid() {
        Auction auction = activeAuction();
        auction.placeBid(BUYER_1, Money.of(BigDecimal.valueOf(15)));
        assertThat(auction.getCurrentPrice().getAmount().compareTo(BigDecimal.valueOf(15)))
                .isZero();

        auction.placeBid(BUYER_2, Money.of(BigDecimal.valueOf(25)));
        assertThat(auction.getCurrentPrice().getAmount().compareTo(BigDecimal.valueOf(25)))
                .isZero();
    }

    // ---------------------------------------------------------------
    // highestBid / bidCount helpers
    // ---------------------------------------------------------------

    @Test
    @DisplayName("highestBid returns empty when no bids placed")
    void highestBid_noBids_empty() {
        assertThat(activeAuction().highestBid()).isEmpty();
    }

    @Test
    @DisplayName("bidCount increments with each accepted bid")
    void bidCount_incrementsOnBid() {
        Auction auction = activeAuction();
        assertThat(auction.bidCount()).isZero();
        auction.placeBid(BUYER_1, Money.of(BigDecimal.valueOf(11)));
        assertThat(auction.bidCount()).isEqualTo(1);
        auction.placeBid(BUYER_2, Money.of(BigDecimal.valueOf(12)));
        assertThat(auction.bidCount()).isEqualTo(2);
    }

    // ---------------------------------------------------------------
    // State transition helpers: markRemoved, markCompleted, revertToEnded
    // ---------------------------------------------------------------

    @Test
    @DisplayName("markRemoved sets status to REMOVED from ACTIVE")
    void markRemoved_setsRemovedStatus() {
        Auction auction = activeAuction();
        auction.markRemoved();
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.REMOVED);
    }

    @Test
    @DisplayName("markCompleted transitions SOLD → COMPLETED")
    void markCompleted_soldToCompleted() {
        Auction auction = activeAuction(); // reserve = 20
        auction.placeBid(BUYER_1, Money.of(BigDecimal.valueOf(20)));
        auction.close(); // → SOLD
        auction.markCompleted();
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.COMPLETED);
    }

    @Test
    @DisplayName("revertToEnded transitions SOLD → ENDED (compensating after PaymentFailed)")
    void revertToEnded_soldToEnded() {
        Auction auction = activeAuction();
        auction.placeBid(BUYER_1, Money.of(BigDecimal.valueOf(20)));
        auction.close(); // → SOLD
        auction.revertToEnded();
        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.ENDED);
    }
}
