package com.bidhub.catalog.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "indexed_listings")
public class IndexedListing {

    @Id
    private UUID listingId;

    private String title;
    private UUID categoryId;
    private UUID sellerId;
    private BigDecimal currentPrice;
    private String currency = "EUR";

    @Enumerated(EnumType.STRING)
    private ListingStatus status;

    private LocalDateTime endTime;
    private LocalDateTime lastIndexedAt;

    public static IndexedListing fromAuctionCreated(UUID listingId, String title,
            UUID categoryId, UUID sellerId,
            BigDecimal startingPrice,
            LocalDateTime endTime) {
        IndexedListing l = new IndexedListing();
        l.listingId = listingId;
        l.title = title;
        l.categoryId = categoryId;
        l.sellerId = sellerId;
        l.currentPrice = startingPrice;
        l.status = ListingStatus.ACTIVE;
        l.endTime = endTime;
        l.lastIndexedAt = LocalDateTime.now();
        return l;
    }

    public void applyBidPlaced(BigDecimal newPrice) {
        this.currentPrice = newPrice;
        this.lastIndexedAt = LocalDateTime.now();
    }

    public void applyStatusChange(ListingStatus newStatus) {
        this.status = newStatus;
        this.lastIndexedAt = LocalDateTime.now();
    }

    public boolean isSearchable() {
        return this.status == ListingStatus.ACTIVE;
    }

    public UUID getListingId() {
        return listingId;
    }

    public String getTitle() {
        return title;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public UUID getSellerId() {
        return sellerId;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public String getCurrency() {
        return currency;
    }

    public ListingStatus getStatus() {
        return status;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public LocalDateTime getLastIndexedAt() {
        return lastIndexedAt;
    }
}