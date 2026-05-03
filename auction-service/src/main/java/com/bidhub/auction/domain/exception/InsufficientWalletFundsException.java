package com.bidhub.auction.domain.exception;

public class InsufficientWalletFundsException extends RuntimeException {

    public InsufficientWalletFundsException(String message) {
        super(message);
    }
}
