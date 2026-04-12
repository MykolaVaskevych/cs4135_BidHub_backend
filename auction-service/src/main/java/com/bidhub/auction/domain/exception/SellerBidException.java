package com.bidhub.auction.domain.exception;

/** Thrown when a seller attempts to bid on or buy-now their own auction (INV-A8, INV-A3). */
public class SellerBidException extends RuntimeException {

    public SellerBidException(String message) {
        super(message);
    }
}
