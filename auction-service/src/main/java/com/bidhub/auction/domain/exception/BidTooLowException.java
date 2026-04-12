package com.bidhub.auction.domain.exception;

/** Thrown when a bid amount is not strictly greater than the current price (INV-A1). */
public class BidTooLowException extends RuntimeException {

    public BidTooLowException(String message) {
        super(message);
    }
}
