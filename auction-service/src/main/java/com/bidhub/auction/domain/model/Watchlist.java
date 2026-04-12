package com.bidhub.auction.domain.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Watchlist aggregate root. One watchlist per user.
 *
 * <p>INV-W1: cannot watch the same auction twice.<br>
 * INV-W2: closed auctions are pruned via {@link #pruneClosed(UUID)}.
 */
@Entity
@Table(name = "watchlists")
public class Watchlist {

    @Id
    @Column(name = "watchlist_id", nullable = false, updatable = false)
    private UUID watchlistId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "watchlist_auctions",
            joinColumns = @JoinColumn(name = "watchlist_id"))
    @Column(name = "auction_id")
    private Set<UUID> watchedAuctionIds = new HashSet<>();

    /** JPA no-arg constructor. */
    protected Watchlist() {}

    public static Watchlist create(UUID userId) {
        Watchlist wl = new Watchlist();
        wl.watchlistId = UUID.randomUUID();
        wl.userId = userId;
        wl.watchedAuctionIds = new HashSet<>();
        return wl;
    }

    /**
     * Adds an auction to the watchlist.
     *
     * <p>INV-W1: duplicate add is rejected. Whether the auction is ACTIVE is verified by
     * WatchlistService before calling this method (cross-aggregate, INV-W2 add side).
     */
    public void addAuction(UUID auctionId) {
        if (watchedAuctionIds.contains(auctionId)) {
            throw new IllegalArgumentException(
                    "Auction " + auctionId + " is already in the watchlist (INV-W1)");
        }
        watchedAuctionIds.add(auctionId);
    }

    public void removeAuction(UUID auctionId) {
        watchedAuctionIds.remove(auctionId);
    }

    public boolean isWatching(UUID auctionId) {
        return watchedAuctionIds.contains(auctionId);
    }

    /**
     * Removes a closed auction from the watchlist. Called when a domain event (AuctionSold,
     * AuctionEnded, AuctionCancelled, AuctionCompleted) is received.
     */
    public void pruneClosed(UUID auctionId) {
        watchedAuctionIds.remove(auctionId);
    }

    public UUID getWatchlistId() {
        return watchlistId;
    }

    public UUID getUserId() {
        return userId;
    }

    public Set<UUID> getWatchedAuctionIds() {
        return Collections.unmodifiableSet(watchedAuctionIds);
    }
}
