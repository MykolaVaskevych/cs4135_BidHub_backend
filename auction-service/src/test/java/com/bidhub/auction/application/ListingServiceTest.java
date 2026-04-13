package com.bidhub.auction.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bidhub.auction.application.dto.CreateListingRequest;
import com.bidhub.auction.application.dto.ListingResponse;
import com.bidhub.auction.application.dto.UpdateListingRequest;
import com.bidhub.auction.application.service.ListingService;
import com.bidhub.auction.domain.exception.ListingNotFoundException;
import com.bidhub.auction.domain.model.Auction;
import com.bidhub.auction.domain.model.AuctionDuration;
import com.bidhub.auction.domain.model.AuctionStatus;
import com.bidhub.auction.domain.model.Listing;
import com.bidhub.auction.domain.model.Money;
import com.bidhub.auction.domain.repository.AuctionRepository;
import com.bidhub.auction.domain.repository.ListingRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ListingServiceTest {

    @Mock private ListingRepository listingRepository;
    @Mock private AuctionRepository auctionRepository;
    @InjectMocks private ListingService listingService;

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID LISTING_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();

    private Listing sampleListing() {
        return Listing.create(SELLER_ID, "Camera", "Nice camera", List.of("p1.jpg"), CATEGORY_ID);
    }

    @Test
    @DisplayName("createListing saves and returns a ListingResponse")
    void createListing_happyPath() {
        when(listingRepository.save(any(Listing.class))).thenAnswer(i -> i.getArgument(0));

        CreateListingRequest req =
                new CreateListingRequest("Camera", "Nice camera", List.of("p1.jpg"), CATEGORY_ID);
        ListingResponse response = listingService.createListing(SELLER_ID, req);

        assertThat(response.title()).isEqualTo("Camera");
        assertThat(response.sellerId()).isEqualTo(SELLER_ID);
        assertThat(response.isActive()).isTrue();
        verify(listingRepository).save(any(Listing.class));
    }

    @Test
    @DisplayName("getListing throws ListingNotFoundException when listing does not exist")
    void getListing_notFound_throws() {
        when(listingRepository.findById(LISTING_ID)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> listingService.getListing(LISTING_ID))
                .isInstanceOf(ListingNotFoundException.class);
    }

    @Test
    @DisplayName("getListing returns response when listing exists")
    void getListing_found_returnsResponse() {
        Listing listing = sampleListing();
        when(listingRepository.findById(any())).thenReturn(Optional.of(listing));

        ListingResponse response = listingService.getListing(listing.getListingId());

        assertThat(response.title()).isEqualTo("Camera");
    }

    @Test
    @DisplayName("INV-L2: updateListing succeeds when auction has zero bids")
    void updateListing_noBids_succeeds() {
        Listing listing = sampleListing();
        when(listingRepository.findById(any())).thenReturn(Optional.of(listing));

        Auction auction =
                Auction.create(
                        listing.getListingId(),
                        SELLER_ID,
                        Money.of(BigDecimal.TEN),
                        Money.of(BigDecimal.valueOf(20)),
                        null,
                        AuctionDuration.of(Instant.now(), Instant.now().plusSeconds(3600)));
        when(auctionRepository.findByListingId(listing.getListingId()))
                .thenReturn(Optional.of(auction));
        when(listingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdateListingRequest req =
                new UpdateListingRequest("New Title", "New Desc", List.of("p2.jpg"), CATEGORY_ID);
        ListingResponse response = listingService.updateListing(SELLER_ID, listing.getListingId(), req);

        assertThat(response.title()).isEqualTo("New Title");
    }

    @Test
    @DisplayName("INV-L2: updateListing throws when auction has bids")
    void updateListing_hasBids_throws() {
        Listing listing = sampleListing();
        when(listingRepository.findById(any())).thenReturn(Optional.of(listing));

        Auction auction =
                Auction.create(
                        listing.getListingId(),
                        SELLER_ID,
                        Money.of(BigDecimal.TEN),
                        Money.of(BigDecimal.valueOf(20)),
                        null,
                        AuctionDuration.of(Instant.now(), Instant.now().plusSeconds(3600)));
        auction.placeBid(UUID.randomUUID(), Money.of(BigDecimal.valueOf(11)));
        when(auctionRepository.findByListingId(listing.getListingId()))
                .thenReturn(Optional.of(auction));

        UpdateListingRequest req =
                new UpdateListingRequest("New Title", "New Desc", List.of("p2.jpg"), CATEGORY_ID);
        assertThatThrownBy(() -> listingService.updateListing(SELLER_ID, listing.getListingId(), req))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("deactivateListing deactivates and saves the listing")
    void deactivateListing_setsInactiveAndSaves() {
        Listing listing = sampleListing();
        when(listingRepository.findById(any())).thenReturn(Optional.of(listing));
        when(listingRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        listingService.deactivateListing(SELLER_ID, listing.getListingId());

        assertThat(listing.isActive()).isFalse();
        verify(listingRepository).save(listing);
    }

    @Test
    @DisplayName("updateListing throws ListingNotFoundException when listing does not exist")
    void updateListing_listingNotFound_throws() {
        when(listingRepository.findById(any())).thenReturn(Optional.empty());
        UpdateListingRequest req =
                new UpdateListingRequest("T", "D", List.of("p.jpg"), CATEGORY_ID);
        assertThatThrownBy(() -> listingService.updateListing(SELLER_ID, LISTING_ID, req))
                .isInstanceOf(ListingNotFoundException.class);
    }
}
