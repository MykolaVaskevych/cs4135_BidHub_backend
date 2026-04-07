package com.bidhub.account.exception;

import java.util.UUID;

public class AddressNotFoundException extends RuntimeException {

    public AddressNotFoundException(UUID userId, UUID addressId) {
        super("Address " + addressId + " not found for user " + userId);
    }
}
