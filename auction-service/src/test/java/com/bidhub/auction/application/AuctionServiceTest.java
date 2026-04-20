package com.bidhub.auction.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bidhub.auction.application.dto.AuctionResponse;
import com.bidhub.auction.application.dto.BidResponse;
import com.bidhub.auction.application.dto.CreateAuctionRequest;
import com.bidhub.auction.application.dto.PlaceBidRequest;
import com.bidhub.auction.application.service.AuctionService;
import com.bidhub.auction.domain.exception.AuctionNotFoundException;
import com.bidhub.auction.domain.exception.IllegalAuctionStateException;
import com.bidhub.auction.domain.exception.ListingNotFoundException;
import com.bidhub.auction.domain.model.Auction;
import com.bidhub.auction.domain.model.AuctionDuration;
import com.bidhub.auction.domain.model.AuctionStatus;
import com.bidhub.auction.domain.model.BidderRef;
import com.bidhub.auction.domain.model.Listing;
import com.bidhub.auction.domain.model.Money;
import com.bidhub.auction.domain.repository.AuctionRepository;
import com.bidhub.auction.domain.repository.ListingRepository;
import com.bidhub.auction.domain.service.BidValidationService;
import com.bidhub.auction.infrastructure.acl.DeliveryClient;
import com.bidhub.auction.infrastructure.acl.NotificationClient;
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
class AuctionServiceTest {

    @Mock private AuctionRepository auctionRepository;
    @Mock private ListingRepository listingRepository;
    @Mock private BidValidationService bidValidationService;
    @Mock private DeliveryClient deliveryClient;
    @Mock private NotificationClient notificationClient;
    @InjectMocks private AuctionService auctionService;

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID BUYER_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();

    private Listing sampleListing() {
        return Listing.create(SELLER_ID, "Camera", "Nice camera", List.of("p1.jpg"), CATEGORY_ID);
    }

    private Auction activeAuction(UUID listingId) {
        return Auction.create(
                listingId,
                SELLER_ID,
                Money.of(BigDecimal.TEN),
                Money.of(BigDecimal.valueOf(20)),
                null,
                AuctionDuration.of(Instant.now(), Instant.now().plusSeconds(3600)));
    }

    @Test
    @DisplayName("createAuction saves auction and returns AuctionResponse")
    void createAuction_happyPath() {
        Listing listing = sampleListing();
        when(listingRepository.findById(listing.getListingId()))
                .thenReturn(Optional.of(listing));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(i -> i.getArgument(0));

        CreateAuctionRequest req =
                new CreateAuctionRequest(
                        listing.getListingId(),
                        BigDecimal.TEN,
                        BigDecimal.valueOf(20),
                        null,
                        Instant.now().plusSeconds(3600));
        AuctionResponse response = auctionService.createAuction(SELLER_ID, req);

        assertThat(response.listingId()).isEqualTo(listing.getListingId());
        assertThat(response.status()).isEqualTo(AuctionStatus.ACTIVE);
        verify(auctionRepository).save(any(Auction.class));
    }

    @Test
    @DisplayName("createAuction throws ListingNotFoundException when listing does not exist")
    void createAuction_listingNotFound_throws() {
        UUID missingId = UUID.randomUUID();
        when(listingRepository.findById(missingId)).thenReturn(Optional.empty());

        CreateAuctionRequest req =
                new CreateAuctionRequest(
                        missingId,
                        BigDecimal.TEN,
                        BigDecimal.valueOf(20),
                        null,
                        Instant.now().plusSeconds(3600));
        assertThatThrownBy(() -> auctionService.createAuction(SELLER_ID, req))
                .isInstanceOf(ListingNotFoundException.class);
    }

    @Test
    @DisplayName("getAuction returns response when auction exists")
    void getAuction_found_returnsResponse() {
        Listing listing = sampleListing();
        Auction auction = activeAuction(listing.getListingId());
        when(auctionRepository.findById(auction.getAuctionId()))
                .thenReturn(Optional.of(auction));

        AuctionResponse response = auctionService.getAuction(auction.getAuctionId());

        assertThat(response.auctionId()).isEqualTo(auction.getAuctionId());
        assertThat(response.status()).isEqualTo(AuctionStatus.ACTIVE);
    }

    @Test
    @DisplayName("getAuction throws AuctionNotFoundException when not found")
    void getAuction_notFound_throws() {
        UUID missing = UUID.randomUUID();
        when(auctionRepository.findById(missing)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> auctionService.getAuction(missing))
                .isInstanceOf(AuctionNotFoundException.class);
    }

    @Test
    @DisplayName("INV-A4: cancelAuction succeeds when ACTIVE and no bids")
    void cancelAuction_noBids_succeeds() {
        Listing listing = sampleListing();
        Auction auction = activeAuction(listing.getListingId());
        when(auctionRepository.findById(auction.getAuctionId()))
                .thenReturn(Optional.of(auction));
        when(auctionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AuctionResponse response = auctionService.cancelAuction(SELLER_ID, auction.getAuctionId());

        assertThat(response.status()).isEqualTo(AuctionStatus.CANCELLED);
    }

    @Test
    @DisplayName("INV-A4: cancelAuction throws when auction has bids")
    void cancelAuction_hasBids_throws() {
        Listing listing = sampleListing();
        Auction auction = activeAuction(listing.getListingId());
        auction.placeBid(BUYER_ID, Money.of(BigDecimal.valueOf(11)));
        when(auctionRepository.findById(auction.getAuctionId()))
                .thenReturn(Optional.of(auction));

        assertThatThrownBy(() -> auctionService.cancelAuction(SELLER_ID, auction.getAuctionId()))
                .isInstanceOf(IllegalAuctionStateException.class);
    }

    @Test
    @DisplayName("placeBid succeeds when bidder is active and bid is valid (INV-A1, A2, A7, A8)")
    void placeBid_activeBidder_succeeds() {
        Listing listing = sampleListing();
        Auction auction = activeAuction(listing.getListingId());
        when(auctionRepository.findById(auction.getAuctionId()))
                .thenReturn(Optional.of(auction));
        when(bidValidationService.validateBidder(BUYER_ID))
                .thenReturn(BidderRef.active(BUYER_ID));
        when(auctionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PlaceBidRequest req = new PlaceBidRequest(BigDecimal.valueOf(15));
        BidResponse response = auctionService.placeBid(BUYER_ID, auction.getAuctionId(), req);

        assertThat(response.bidderId()).isEqualTo(BUYER_ID);
        assertThat(response.amount().amount().compareTo(BigDecimal.valueOf(15))).isZero();
    }
}
