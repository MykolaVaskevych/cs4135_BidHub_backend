package com.bidhub.delivery.application.dto;

import com.bidhub.delivery.domain.model.DeliveryJob;
import com.bidhub.delivery.domain.model.DeliveryStatus;

import java.time.Instant;
import java.util.UUID;

public record DeliveryJobResponse(
        UUID deliveryJobId,
        UUID orderId,
        UUID sellerId,
        UUID buyerId,
        UUID driverId,
        DeliveryStatus status,
        AddressDto pickupAddress,
        AddressDto deliveryAddress,
        Instant createdAt,
        Instant updatedAt
) {
    public static DeliveryJobResponse from(DeliveryJob job) {
        return new DeliveryJobResponse(
                job.getDeliveryJobId(),
                job.getOrderId(),
                job.getSellerId(),
                job.getBuyerId(),
                job.getDriverId(),
                job.getStatus(),
                AddressDto.from(job.getPickupAddress()),
                AddressDto.from(job.getDeliveryAddress()),
                job.getCreatedAt(),
                job.getUpdatedAt()
        );
    }
}
