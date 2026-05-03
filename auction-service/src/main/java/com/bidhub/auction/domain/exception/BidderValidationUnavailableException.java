package com.bidhub.auction.domain.exception;

/**
 * Thrown when bidder eligibility cannot be confirmed because account-service is
 * unreachable, returns no body, the call times out, or the circuit breaker is open.
 *
 * <p>Maps to HTTP 503 in {@code GlobalExceptionHandler}. The bid is rejected and
 * no row is persisted; the client may retry once account-service is back.
 */
public class BidderValidationUnavailableException extends RuntimeException {

    public BidderValidationUnavailableException(String message) {
        super(message);
    }

    public BidderValidationUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
