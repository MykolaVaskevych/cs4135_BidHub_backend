package com.bidhub.auction;

import static org.assertj.core.api.Assertions.assertThat;

import com.bidhub.auction.application.dto.AuctionResponse;
import com.bidhub.auction.application.dto.BidResponse;
import com.bidhub.auction.application.dto.CreateAuctionRequest;
import com.bidhub.auction.application.dto.CreateListingRequest;
import com.bidhub.auction.application.dto.ListingResponse;
import com.bidhub.auction.application.dto.PlaceBidRequest;
import com.bidhub.auction.application.service.AuctionService;
import com.bidhub.auction.application.service.ListingService;
import com.bidhub.auction.domain.model.AuctionStatus;
import com.bidhub.auction.domain.model.BidderRef;
import com.bidhub.auction.domain.service.BidValidationService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.Mockito.when;

/**
 * Full integration test: Spring context + H2 in-memory database.
 *
 * <p>Scenario: create listing → create auction → place bid → read auction → assert currentPrice updated.
 *
 * <p>BidValidationService is mocked because the external account-service call (Phase 6) is blocked
 * on Xunze's GET /api/accounts/{userId} endpoint.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class AuctionServiceIntegrationTest {

    @Autowired private ListingService listingService;
    @Autowired private AuctionService auctionService;

    // BidValidationService HTTP impl is not yet wired (Phase 6 blocked) — stub it
    @MockitoBean private BidValidationService bidValidationService;

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID BUYER_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();

    @Test
    @DisplayName("Integration: create listing → create auction → place bid → currentPrice updated")
    void fullBiddingFlow_updatesCurrentPrice() {
        // 1. Create listing
        CreateListingRequest listingReq =
                new CreateListingRequest("Canon 5D", "DSLR camera", List.of("canon.jpg"), CATEGORY_ID);
        ListingResponse listing = listingService.createListing(SELLER_ID, listingReq);
        assertThat(listing.isActive()).isTrue();

        // 2. Create auction for the listing
        CreateAuctionRequest auctionReq =
                new CreateAuctionRequest(
                        listing.listingId(),
                        BigDecimal.TEN,
                        BigDecimal.valueOf(50),
                        null,
                        Instant.now().plusSeconds(7200));
        AuctionResponse auction = auctionService.createAuction(SELLER_ID, auctionReq);
        assertThat(auction.status()).isEqualTo(AuctionStatus.ACTIVE);
        assertThat(auction.currentPrice().amount().compareTo(BigDecimal.TEN)).isZero();

        // 3. Place bid (mock bidder as active — ACL impl is Phase 6)
        when(bidValidationService.validateBidder(BUYER_ID)).thenReturn(BidderRef.active(BUYER_ID));
        PlaceBidRequest bidReq = new PlaceBidRequest(BigDecimal.valueOf(25));
        BidResponse bid = auctionService.placeBid(BUYER_ID, auction.auctionId(), bidReq);
        assertThat(bid.bidderId()).isEqualTo(BUYER_ID);
        assertThat(bid.amount().amount().compareTo(BigDecimal.valueOf(25))).isZero();
        assertThat(bid.isWinning()).isTrue();

        // 4. Read auction — currentPrice must reflect the bid
        AuctionResponse updated = auctionService.getAuction(auction.auctionId());
        assertThat(updated.currentPrice().amount().compareTo(BigDecimal.valueOf(25))).isZero();
        assertThat(updated.bidCount()).isEqualTo(1);
    }
}
