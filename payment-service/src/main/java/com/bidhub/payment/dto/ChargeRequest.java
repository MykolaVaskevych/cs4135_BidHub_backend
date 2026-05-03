package com.bidhub.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ChargeRequest(
        @NotNull @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
                BigDecimal amount,
        String description) {}
