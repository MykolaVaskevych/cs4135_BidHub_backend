package com.bidhub.auction.domain.model;

import java.util.UUID;

/**
 * ACL representation of Account context's User.
 *
 * <p>Returned by BidValidationService after a live REST call. Auction context never imports
 * Account's internal domain model.
 */
public record BidderRef(UUID userId, boolean isActive) {

    public static BidderRef active(UUID userId) {
        return new BidderRef(userId, true);
    }

    public static BidderRef inactive(UUID userId) {
        return new BidderRef(userId, false);
    }
}
