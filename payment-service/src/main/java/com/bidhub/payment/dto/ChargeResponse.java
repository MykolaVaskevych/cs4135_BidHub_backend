package com.bidhub.payment.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ChargeResponse(
        UUID transactionId,
        UUID walletId,
        UUID userId,
        BigDecimal balance,
        String currency) {}
