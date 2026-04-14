package com.bidhub.delivery.domain.exception;

import java.util.UUID;

public class DeliveryJobNotFoundException extends RuntimeException {
    public DeliveryJobNotFoundException(UUID jobId) {
        super("Delivery job not found: " + jobId);
    }
}
