package com.bidhub.auction.domain.exception;

/**
 * Thrown when an operation is attempted on an auction that is not in the required state
 * (INV-A2, INV-A3, INV-A4).
 */
public class IllegalAuctionStateException extends RuntimeException {

    public IllegalAuctionStateException(String message) {
        super(message);
    }
}
