package com.bidhub.auction.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bidhub.auction.domain.model.Listing;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ListingInvariantTest {

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();

    private Listing validListing() {
        return Listing.create(
                SELLER_ID,
                "Vintage Camera",
                "A great camera",
                List.of("photo1.jpg"),
                CATEGORY_ID);
    }

    // ---------------------------------------------------------------
    // INV-L1: categoryId must be present (entity-level; active check
    // is enforced via CategoryRef cache at application service layer)
    // ---------------------------------------------------------------

    @Test
    @DisplayName("INV-L1: creating a listing with a null categoryId is rejected")
    void invL1_nullCategoryId_rejected() {
        assertThatThrownBy(
                        () ->
                                Listing.create(
                                        SELLER_ID,
                                        "Title",
                                        "Desc",
                                        List.of("photo.jpg"),
                                        null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------------------------------------------------------------
    // INV-L2: listing can only be updated if associated auction has
    // zero bids. This is a CROSS-AGGREGATE invariant enforced at the
    // APPLICATION SERVICE layer (ListingService), NOT inside Listing
    // itself. Tested in ListingServiceTest (Phase 4).
    // Documented here for traceability.
    // ---------------------------------------------------------------

    // ---------------------------------------------------------------
    // INV-L3: at least one photo required
    // ---------------------------------------------------------------

    @Test
    @DisplayName("INV-L3: creating a listing with empty photos is rejected")
    void invL3_emptyPhotos_rejected() {
        assertThatThrownBy(
                        () ->
                                Listing.create(
                                        SELLER_ID,
                                        "Title",
                                        "Desc",
                                        List.of(),
                                        CATEGORY_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("INV-L3: creating a listing with null photos is rejected")
    void invL3_nullPhotos_rejected() {
        assertThatThrownBy(
                        () ->
                                Listing.create(
                                        SELLER_ID, "Title", "Desc", null, CATEGORY_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("INV-L3: updating a listing to have no photos is rejected")
    void invL3_updateToEmptyPhotos_rejected() {
        Listing listing = validListing();
        assertThatThrownBy(
                        () ->
                                listing.update(
                                        "New Title",
                                        "New Desc",
                                        List.of(),
                                        CATEGORY_ID))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("INV-L3: valid listing with one photo is created successfully")
    void invL3_onePhoto_accepted() {
        Listing listing = validListing();
        assertThat(listing).isNotNull();
    }

    // ---------------------------------------------------------------
    // Deactivate / activate transitions
    // ---------------------------------------------------------------

    @Test
    @DisplayName("deactivate sets isActive to false")
    void deactivate_setsInactive() {
        Listing listing = validListing();
        listing.deactivate();
        assertThat(listing.isActive()).isFalse();
    }

    @Test
    @DisplayName("activate sets isActive to true")
    void activate_setsActive() {
        Listing listing = validListing();
        listing.deactivate();
        listing.activate();
        assertThat(listing.isActive()).isTrue();
    }
}
