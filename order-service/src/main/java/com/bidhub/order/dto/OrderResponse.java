package com.bidhub.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderResponse(
        UUID orderId,
        UUID auctionId,
        UUID buyerId,
        UUID sellerId,
        BigDecimal amount,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
