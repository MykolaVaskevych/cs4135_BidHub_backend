package com.bidhub.auction.domain.model;

public enum AuctionStatus {
    /** Auction created; transitions to ACTIVE immediately on publish. */
    PENDING,
    /** Accepting bids. */
    ACTIVE,
    /** Sold to highest bidder (reserve met or buy-now executed). */
    SOLD,
    /** Auction ended without meeting reserve. */
    ENDED,
    /** Cancelled by seller (zero bids only). */
    CANCELLED,
    /** Removed by admin moderation. */
    REMOVED,
    /** Settlement fully completed. Terminal state. */
    COMPLETED
}
