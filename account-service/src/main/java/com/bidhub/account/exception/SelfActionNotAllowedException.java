package com.bidhub.account.exception;

public class SelfActionNotAllowedException extends RuntimeException {

    public SelfActionNotAllowedException(String action) {
        super("Admin cannot " + action + " their own account");
    }
}
