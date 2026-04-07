package com.bidhub.account.exception;

import com.bidhub.account.model.AccountStatus;

public class AccountNotActiveException extends RuntimeException {

    public AccountNotActiveException(AccountStatus status) {
        super("Account is not active: " + status);
    }
}
