package com.bidhub.delivery.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateDeliveryJobRequest(
        @NotNull UUID orderId,
        UUID auctionId,
        @NotNull UUID sellerId,
        @NotNull UUID buyerId,
        @NotNull @Valid AddressDto pickupAddress,
        @NotNull @Valid AddressDto deliveryAddress,
        @NotNull UUID escrowId,
        @NotNull @Positive BigDecimal escrowAmount
) {}
