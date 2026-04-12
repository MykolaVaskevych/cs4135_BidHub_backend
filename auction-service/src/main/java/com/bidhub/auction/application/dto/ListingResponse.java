package com.bidhub.auction.application.dto;

import com.bidhub.auction.domain.model.Listing;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ListingResponse(
        UUID listingId,
        UUID sellerId,
        String title,
        String description,
        List<String> photos,
        UUID categoryId,
        boolean isActive,
        Instant createdAt) {

    public static ListingResponse from(Listing listing) {
        return new ListingResponse(
                listing.getListingId(),
                listing.getSellerId(),
                listing.getTitle(),
                listing.getDescription(),
                listing.getPhotos(),
                listing.getCategoryId(),
                listing.isActive(),
                listing.getCreatedAt());
    }
}
