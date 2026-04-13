package com.bidhub.auction.domain.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Listing aggregate root. Represents the item being auctioned — the product data.
 *
 * <p>Separate aggregate from Auction: it has its own identity and can be edited independently.
 *
 * <p>INV-L1: categoryId must not be null (active check via CategoryRef cache is at app service
 * layer).<br>
 * INV-L2: updates are blocked if the associated auction has bids — enforced at application service
 * layer (cross-aggregate invariant).<br>
 * INV-L3: at least one photo required.
 */
@Entity
@Table(name = "listings")
public class Listing {

    @Id
    @Column(name = "listing_id", nullable = false, updatable = false)
    private UUID listingId;

    @Column(name = "seller_id", nullable = false, updatable = false)
    private UUID sellerId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = false)
    private String description;

    @ElementCollection
    @CollectionTable(name = "listing_photos", joinColumns = @JoinColumn(name = "listing_id"))
    @OrderColumn(name = "photo_order")
    @Column(name = "photo_url")
    private List<String> photos;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** JPA no-arg constructor. */
    protected Listing() {}

    /** Factory method. New listings are active by default. */
    public static Listing create(
            UUID sellerId,
            String title,
            String description,
            List<String> photos,
            UUID categoryId) {

        // INV-L1: categoryId must not be null
        if (categoryId == null) {
            throw new IllegalArgumentException("categoryId must not be null (INV-L1)");
        }
        // INV-L3: at least one photo
        requireAtLeastOnePhoto(photos);

        Listing listing = new Listing();
        listing.listingId = UUID.randomUUID();
        listing.sellerId = sellerId;
        listing.title = title;
        listing.description = description;
        listing.photos = new ArrayList<>(photos);
        listing.categoryId = categoryId;
        listing.isActive = true;
        listing.createdAt = Instant.now();
        return listing;
    }

    /**
     * Updates the listing's mutable fields.
     *
     * <p>INV-L2 (no updates when bids exist) is enforced by ListingService before calling this.
     * INV-L3 (at least one photo) is enforced here.
     */
    public void update(String title, String description, List<String> photos, UUID categoryId) {
        // INV-L3: at least one photo
        requireAtLeastOnePhoto(photos);
        if (categoryId == null) {
            throw new IllegalArgumentException("categoryId must not be null (INV-L1)");
        }
        this.title = title;
        this.description = description;
        this.photos = List.copyOf(photos);
        this.categoryId = categoryId;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }

    private static void requireAtLeastOnePhoto(List<String> photos) {
        if (photos == null || photos.isEmpty()) {
            throw new IllegalArgumentException("At least one photo is required (INV-L3)");
        }
    }

    public UUID getListingId() {
        return listingId;
    }

    public UUID getSellerId() {
        return sellerId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getPhotos() {
        return List.copyOf(photos);
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public boolean isActive() {
        return isActive;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
