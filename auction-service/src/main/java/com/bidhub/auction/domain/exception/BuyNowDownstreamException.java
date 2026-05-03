package com.bidhub.auction.domain.exception;

public class BuyNowDownstreamException extends RuntimeException {

    public BuyNowDownstreamException(String message) {
        super(message);
    }

    public BuyNowDownstreamException(String message, Throwable cause) {
        super(message, cause);
    }
}
