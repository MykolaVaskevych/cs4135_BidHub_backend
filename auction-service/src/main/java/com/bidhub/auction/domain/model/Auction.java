package com.bidhub.auction.domain.model;

import com.bidhub.auction.domain.exception.BidTooLowException;
import com.bidhub.auction.domain.exception.IllegalAuctionStateException;
import com.bidhub.auction.domain.exception.SellerBidException;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Auction aggregate root. Encapsulates bidding mechanics for a single listing.
 *
 * <p>INV-A1: first bid ≥ startingPrice; subsequent bids ≥ currentPrice + €1 (B7)<br>
 * INV-A2: bids only when ACTIVE<br>
 * INV-A3: buy-now only when ACTIVE, price set, buyer ≠ seller<br>
 * INV-A4: cancel only when ACTIVE and bidCount = 0<br>
 * INV-A5: close → SOLD if highestBid ≥ reserve, else ENDED<br>
 * INV-A6: @Version optimistic locking<br>
 * INV-A8: bidderId ≠ sellerId<br>
 * INV-A9: currentPrice = max bid amount or startingPrice
 */
@Entity
@Table(name = "auctions")
public class Auction {

    private static final Money MIN_BID_INCREMENT = Money.of(BigDecimal.ONE);

    @Id
    @Column(name = "auction_id", nullable = false, updatable = false)
    private UUID auctionId;

    @Column(name = "listing_id", nullable = false, updatable = false)
    private UUID listingId;

    /** Denormalised copy of Listing.sellerId — immutable after creation (INV-A8). */
    @Column(name = "seller_id", nullable = false, updatable = false)
    private UUID sellerId;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "starting_price_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "starting_price_currency"))
    })
    private Money startingPrice;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "reserve_price_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "reserve_price_currency"))
    })
    private Money reservePrice;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "buy_now_price_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "buy_now_price_currency"))
    })
    private Money buyNowPrice;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "current_price_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "current_price_currency"))
    })
    private Money currentPrice;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(
                name = "startTime",
                column = @Column(name = "auction_start_time")),
        @AttributeOverride(name = "endTime", column = @Column(name = "auction_end_time"))
    })
    private AuctionDuration duration;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AuctionStatus status;

    /** Optimistic locking — prevents concurrent bid races (INV-A6). */
    @Version
    @Column(name = "version")
    private Long version;

    @OneToMany(mappedBy = "auction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Bid> bids = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** JPA no-arg constructor. */
    protected Auction() {}

    /**
     * Factory method. Creates an auction in ACTIVE state (PENDING is transient per domain model).
     */
    public static Auction create(
            UUID listingId,
            UUID sellerId,
            Money startingPrice,
            Money reservePrice,
            Money buyNowPrice,
            AuctionDuration duration) {

        Auction auction = new Auction();
        auction.auctionId = UUID.randomUUID();
        auction.listingId = listingId;
        auction.sellerId = sellerId;
        auction.startingPrice = startingPrice;
        auction.reservePrice = reservePrice;
        auction.buyNowPrice = buyNowPrice;
        auction.currentPrice = startingPrice; // INV-A9: starts at startingPrice
        auction.duration = duration;
        auction.status = AuctionStatus.ACTIVE;
        auction.createdAt = Instant.now();
        return auction;
    }

    // ---------------------------------------------------------------
    // Domain operations
    // ---------------------------------------------------------------

    /**
     * Places a bid. Enforces INV-A1, INV-A2, INV-A8, INV-A9.
     *
     * <p>INV-A7 (bidder not BANNED/SUSPENDED) is validated by BidValidationService ACL before this
     * method is called.
     */
    public Bid placeBid(UUID bidderId, Money amount) {
        // INV-A2: must be ACTIVE
        if (status != AuctionStatus.ACTIVE) {
            throw new IllegalAuctionStateException(
                    "Bids are only accepted on ACTIVE auctions; current status: " + status);
        }
        // INV-A8: seller cannot bid on own auction
        if (bidderId.equals(sellerId)) {
            throw new SellerBidException("Seller cannot bid on their own auction");
        }
        // INV-A1 (B7): first bid must reach startingPrice; subsequent bids must clear
        // currentPrice by at least €1.
        Money minimumBid = bids.isEmpty() ? startingPrice : currentPrice.add(MIN_BID_INCREMENT);
        if (!amount.isGreaterThanOrEqualTo(minimumBid)) {
            throw new BidTooLowException(
                    "Bid " + amount + " is below the minimum acceptable bid " + minimumBid);
        }

        // Mark previous winning bid as non-winning
        bids.forEach(b -> b.setWinning(false));

        Bid bid = new Bid(this, bidderId, amount);
        bids.add(bid);

        // INV-A9: currentPrice = max bid amount
        currentPrice = amount;

        return bid;
    }

    /**
     * Executes a buy-now purchase. Enforces INV-A3.
     *
     * <p>Transitions auction to SOLD and records the buyer as the winner.
     */
    public void buyNow(UUID buyerId) {
        // INV-A3: must be ACTIVE
        if (status != AuctionStatus.ACTIVE) {
            throw new IllegalAuctionStateException(
                    "Buy-now is only available on ACTIVE auctions; current status: " + status);
        }
        // INV-A3: buyNowPrice must be set
        if (buyNowPrice == null) {
            throw new IllegalAuctionStateException("This auction does not have a buy-now price");
        }
        // INV-A3: buyer ≠ seller
        if (buyerId.equals(sellerId)) {
            throw new SellerBidException("Seller cannot buy-now their own auction");
        }

        currentPrice = buyNowPrice;
        status = AuctionStatus.SOLD;
    }

    /**
     * Closes the auction. Enforces INV-A5: SOLD if highestBid ≥ reserve, else ENDED.
     *
     * <p>Called by AuctionClosingService when endTime is reached.
     */
    public void close() {
        Optional<Bid> highest = highestBid();
        if (highest.isPresent() && highest.get().getAmount().isGreaterThanOrEqualTo(reservePrice)) {
            status = AuctionStatus.SOLD;
        } else {
            status = AuctionStatus.ENDED;
        }
    }

    /**
     * Cancels the auction. Enforces INV-A4: only when ACTIVE and no bids.
     */
    public void cancel() {
        if (status != AuctionStatus.ACTIVE) {
            throw new IllegalAuctionStateException(
                    "Can only cancel ACTIVE auctions; current status: " + status);
        }
        if (!bids.isEmpty()) {
            throw new IllegalAuctionStateException(
                    "Cannot cancel auction that already has " + bids.size() + " bid(s)");
        }
        status = AuctionStatus.CANCELLED;
    }

    /** Marks the auction as removed by admin moderation. */
    public void markRemoved() {
        status = AuctionStatus.REMOVED;
    }

    /** Transitions SOLD → COMPLETED after settlement is complete. */
    public void markCompleted() {
        if (status != AuctionStatus.SOLD) {
            throw new IllegalAuctionStateException(
                    "Can only complete SOLD auctions; current status: " + status);
        }
        status = AuctionStatus.COMPLETED;
    }

    /** Compensating transition: SOLD → ENDED when payment fails (saga reversal). */
    public void revertToEnded() {
        if (status != AuctionStatus.SOLD) {
            throw new IllegalAuctionStateException(
                    "Can only revert SOLD auctions to ENDED; current status: " + status);
        }
        status = AuctionStatus.ENDED;
    }

    public boolean isExpired() {
        return duration.isExpired();
    }

    /** Returns the current winning bid, or empty if no bids have been placed. */
    public Optional<Bid> highestBid() {
        return bids.stream().max(Comparator.comparing(b -> b.getAmount().getAmount()));
    }

    public int bidCount() {
        return bids.size();
    }

    // ---------------------------------------------------------------
    // Getters
    // ---------------------------------------------------------------

    public UUID getAuctionId() {
        return auctionId;
    }

    public UUID getListingId() {
        return listingId;
    }

    public UUID getSellerId() {
        return sellerId;
    }

    public Money getStartingPrice() {
        return startingPrice;
    }

    public Money getReservePrice() {
        return reservePrice;
    }

    public Money getBuyNowPrice() {
        return buyNowPrice;
    }

    public Money getCurrentPrice() {
        return currentPrice;
    }

    public AuctionDuration getDuration() {
        return duration;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public Long getVersion() {
        return version;
    }

    public List<Bid> getBids() {
        return Collections.unmodifiableList(bids);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
