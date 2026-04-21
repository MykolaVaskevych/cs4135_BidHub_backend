package com.bidhub.catalog.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record IndexListingRequest(
        UUID listingId,
        String title,
        UUID categoryId,
        UUID sellerId,
        BigDecimal startingPrice,
        Instant endTime) {}
