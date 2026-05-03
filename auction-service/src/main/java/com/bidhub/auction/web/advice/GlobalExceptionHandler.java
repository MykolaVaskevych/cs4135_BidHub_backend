package com.bidhub.auction.web.advice;

import com.bidhub.auction.domain.exception.AuctionNotFoundException;
import com.bidhub.auction.domain.exception.BidTooLowException;
import com.bidhub.auction.domain.exception.BidderValidationUnavailableException;
import com.bidhub.auction.domain.exception.BuyNowDownstreamException;
import com.bidhub.auction.domain.exception.IllegalAuctionStateException;
import com.bidhub.auction.domain.exception.InsufficientWalletFundsException;
import com.bidhub.auction.domain.exception.ListingNotFoundException;
import com.bidhub.auction.domain.exception.MissingShippingAddressException;
import com.bidhub.auction.domain.exception.SellerBidException;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BidTooLowException.class)
    public ProblemDetail handleBidTooLow(BidTooLowException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Bid Too Low");
        return pd;
    }

    @ExceptionHandler(SellerBidException.class)
    public ProblemDetail handleSellerBid(SellerBidException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Seller Cannot Bid");
        return pd;
    }

    @ExceptionHandler(IllegalAuctionStateException.class)
    public ProblemDetail handleIllegalState(IllegalAuctionStateException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Illegal Auction State");
        return pd;
    }

    @ExceptionHandler({AuctionNotFoundException.class, ListingNotFoundException.class})
    public ProblemDetail handleNotFound(RuntimeException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Not Found");
        return pd;
    }

    @ExceptionHandler(BidderValidationUnavailableException.class)
    public ProblemDetail handleBidderValidationUnavailable(BidderValidationUnavailableException ex) {
        ProblemDetail pd =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        pd.setTitle("Bidder Validation Unavailable");
        pd.setProperty("retryable", true);
        return pd;
    }

    @ExceptionHandler(MissingShippingAddressException.class)
    public ProblemDetail handleMissingShippingAddress(MissingShippingAddressException ex) {
        ProblemDetail pd =
                ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setTitle("Missing Shipping Address");
        pd.setProperty("role", ex.getRole());
        pd.setProperty("userId", ex.getUserId().toString());
        return pd;
    }

    @ExceptionHandler(InsufficientWalletFundsException.class)
    public ProblemDetail handleInsufficientFunds(InsufficientWalletFundsException ex) {
        ProblemDetail pd =
                ProblemDetail.forStatusAndDetail(HttpStatus.PAYMENT_REQUIRED, ex.getMessage());
        pd.setTitle("Insufficient Wallet Funds");
        return pd;
    }

    @ExceptionHandler(BuyNowDownstreamException.class)
    public ProblemDetail handleBuyNowDownstream(BuyNowDownstreamException ex) {
        ProblemDetail pd =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        pd.setTitle("Buy-Now Downstream Failure");
        pd.setProperty("retryable", true);
        return pd;
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(ObjectOptimisticLockingFailureException ex) {
        ProblemDetail pd =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.CONFLICT,
                        "Concurrent modification detected — please retry (INV-A6)");
        pd.setTitle("Optimistic Lock Conflict");
        pd.setProperty("retryable", true);
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors =
                ex.getBindingResult().getFieldErrors().stream()
                        .collect(
                                Collectors.toMap(
                                        fe -> fe.getField(),
                                        fe ->
                                                fe.getDefaultMessage() != null
                                                        ? fe.getDefaultMessage()
                                                        : "invalid",
                                        (a, b) -> a));
        ProblemDetail pd =
                ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        pd.setTitle("Validation Error");
        pd.setProperty("fieldErrors", fieldErrors);
        return pd;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        pd.setTitle("Conflict");
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneral(Exception ex) {
        ProblemDetail pd =
                ProblemDetail.forStatusAndDetail(
                        HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        pd.setTitle("Internal Server Error");
        pd.setProperty("timestamp", Instant.now().toString());
        return pd;
    }
}
