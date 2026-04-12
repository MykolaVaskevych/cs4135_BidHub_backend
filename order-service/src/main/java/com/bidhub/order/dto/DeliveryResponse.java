package com.bidhub.order.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record DeliveryResponse(
        UUID deliveryId,
        UUID orderId,
        UUID driverId,
        LocalDateTime acceptedAt,
        LocalDateTime collectedAt,
        LocalDateTime deliveredAt) {}
