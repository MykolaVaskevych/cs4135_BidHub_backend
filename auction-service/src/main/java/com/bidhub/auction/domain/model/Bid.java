package com.bidhub.auction.domain.model;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Aggregate part of the Auction aggregate. Only created via {@link Auction#placeBid}. Not exposed
 * directly — access through Auction root.
 */
@Entity
@Table(name = "bids")
public class Bid {

    @Id
    @Column(name = "bid_id", nullable = false, updatable = false)
    private UUID bidId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auction_id", nullable = false, updatable = false)
    private Auction auction;

    @Column(name = "bidder_id", nullable = false, updatable = false)
    private UUID bidderId;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "amount", column = @Column(name = "bid_amount")),
        @AttributeOverride(name = "currency", column = @Column(name = "bid_currency"))
    })
    private Money amount;

    @Column(name = "placed_at", nullable = false, updatable = false)
    private Instant placedAt;

    @Column(name = "is_winning")
    private boolean isWinning;

    /** JPA no-arg constructor. */
    protected Bid() {}

    Bid(Auction auction, UUID bidderId, Money amount) {
        this.bidId = UUID.randomUUID();
        this.auction = auction;
        this.bidderId = bidderId;
        this.amount = amount;
        this.placedAt = Instant.now();
        this.isWinning = true;
    }

    public UUID getBidId() {
        return bidId;
    }

    public UUID getBidderId() {
        return bidderId;
    }

    public Money getAmount() {
        return amount;
    }

    public Instant getPlacedAt() {
        return placedAt;
    }

    public boolean isWinning() {
        return isWinning;
    }

    void setWinning(boolean winning) {
        this.isWinning = winning;
    }
}
