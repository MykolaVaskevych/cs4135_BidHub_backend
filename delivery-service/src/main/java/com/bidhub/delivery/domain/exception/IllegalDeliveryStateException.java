package com.bidhub.delivery.domain.exception;

import com.bidhub.delivery.domain.model.DeliveryStatus;

public class IllegalDeliveryStateException extends RuntimeException {

    public IllegalDeliveryStateException(String message) {
        super(message);
    }

    public IllegalDeliveryStateException(String operation, DeliveryStatus current, DeliveryStatus required) {
        super("Operation '" + operation + "' requires status " + required + " but current is " + current);
    }
}
