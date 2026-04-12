package com.bidhub.auction.infrastructure.acl;

import java.util.UUID;

/**
 * Published Language representation of account-service's UserResponse.
 * Only the fields needed by auction-service are mapped here.
 *
 * <p>Contract: GET /api/accounts/{userId} → { userId, status: ACTIVE|SUSPENDED|BANNED|INACTIVE, ... }
 */
public record AccountView(UUID userId, String status) {

    public boolean isEligibleToBid() {
        return "ACTIVE".equalsIgnoreCase(status);
    }
}
