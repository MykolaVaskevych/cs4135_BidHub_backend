package com.bidhub.auction.domain.service;

import com.bidhub.auction.domain.model.BidderRef;
import java.util.UUID;

/**
 * Domain service interface for validating bidder eligibility.
 *
 * <p>Implementation lives in {@code infrastructure.acl.AccountClientBidValidationService} and
 * makes a live REST call to account-service. Injected into AuctionService to enforce INV-A7.
 *
 * <p>Phase 6: implementation wired once Xunze delivers GET /api/accounts/{userId}.
 */
public interface BidValidationService {

    /**
     * Returns a {@link BidderRef} for the given user. Throws or returns inactive ref if the bidder
     * is BANNED or SUSPENDED.
     */
    BidderRef validateBidder(UUID bidderId);
}
