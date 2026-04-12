package com.bidhub.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponse(
        UUID transactionId,
        BigDecimal amount,
        String type,
        String description,
        LocalDateTime createdAt) {}
