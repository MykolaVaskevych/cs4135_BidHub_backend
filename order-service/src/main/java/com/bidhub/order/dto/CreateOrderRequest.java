package com.bidhub.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record CreateOrderRequest(
        @NotNull UUID auctionId,
        @NotNull UUID buyerId,
        @NotNull UUID sellerId,
        @NotNull @DecimalMin("0.01") BigDecimal amount) {}
