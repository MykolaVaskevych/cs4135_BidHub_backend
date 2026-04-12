package com.bidhub.auction.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CreateAuctionRequest(
        @NotNull UUID listingId,
        @NotNull @Positive BigDecimal startingPrice,
        @NotNull @Positive BigDecimal reservePrice,
        BigDecimal buyNowPrice,
        @NotNull Instant endTime) {}
