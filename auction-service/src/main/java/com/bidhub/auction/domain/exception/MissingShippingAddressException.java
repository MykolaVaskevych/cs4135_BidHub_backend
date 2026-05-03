package com.bidhub.auction.domain.exception;

import java.util.UUID;

public class MissingShippingAddressException extends RuntimeException {

    private final UUID userId;
    private final String role;

    public MissingShippingAddressException(UUID userId, String role) {
        super("No default shipping address on file for " + role + " " + userId);
        this.userId = userId;
        this.role = role;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }
}
