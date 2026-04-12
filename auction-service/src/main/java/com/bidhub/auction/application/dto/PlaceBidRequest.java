package com.bidhub.auction.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record PlaceBidRequest(@NotNull @Positive BigDecimal amount) {}
