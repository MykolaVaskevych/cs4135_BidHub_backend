package com.bidhub.auction.application.service;

import com.bidhub.auction.application.dto.AuctionResponse;
import com.bidhub.auction.application.dto.BidResponse;
import com.bidhub.auction.application.dto.CreateAuctionRequest;
import com.bidhub.auction.application.dto.PlaceBidRequest;
import com.bidhub.auction.domain.exception.AuctionNotFoundException;
import com.bidhub.auction.domain.exception.IllegalAuctionStateException;
import com.bidhub.auction.domain.exception.ListingNotFoundException;
import com.bidhub.auction.domain.model.Auction;
import com.bidhub.auction.domain.model.AuctionDuration;
import com.bidhub.auction.domain.model.Bid;
import com.bidhub.auction.domain.model.BidderRef;
import com.bidhub.auction.domain.model.Money;
import com.bidhub.auction.domain.repository.AuctionRepository;
import com.bidhub.auction.domain.repository.ListingRepository;
import com.bidhub.auction.domain.service.BidValidationService;
import com.bidhub.auction.infrastructure.acl.DeliveryClient;
import com.bidhub.auction.infrastructure.acl.NotificationClient;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final ListingRepository listingRepository;
    private final BidValidationService bidValidationService;
    private final DeliveryClient deliveryClient;
    private final NotificationClient notificationClient;

    public AuctionService(
            AuctionRepository auctionRepository,
            ListingRepository listingRepository,
            BidValidationService bidValidationService,
            DeliveryClient deliveryClient,
            NotificationClient notificationClient) {
        this.auctionRepository = auctionRepository;
        this.listingRepository = listingRepository;
        this.bidValidationService = bidValidationService;
        this.deliveryClient = deliveryClient;
        this.notificationClient = notificationClient;
    }

    public AuctionResponse createAuction(UUID sellerId, CreateAuctionRequest req) {
        listingRepository
                .findById(req.listingId())
                .orElseThrow(() -> new ListingNotFoundException(req.listingId()));

        Auction auction =
                Auction.create(
                        req.listingId(),
                        sellerId,
                        Money.of(req.startingPrice()),
                        Money.of(req.reservePrice()),
                        req.buyNowPrice() != null ? Money.of(req.buyNowPrice()) : null,
                        AuctionDuration.of(Instant.now(), req.endTime()));

        return AuctionResponse.from(auctionRepository.save(auction));
    }

    @Transactional(readOnly = true)
    public AuctionResponse getAuction(UUID auctionId) {
        return auctionRepository
                .findById(auctionId)
                .map(AuctionResponse::from)
                .orElseThrow(() -> new AuctionNotFoundException(auctionId));
    }

    @Transactional(readOnly = true)
    public List<AuctionResponse> searchAuctions(String keyword, UUID categoryId) {
        return auctionRepository.searchActive(keyword, categoryId).stream()
                .map(AuctionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuctionResponse> getAllAuctions() {
        return auctionRepository.findAll().stream()
                .map(AuctionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuctionResponse> getMyAuctions(UUID sellerId) {
        return auctionRepository.findBySellerId(sellerId).stream()
                .map(AuctionResponse::from)
                .toList();
    }

    public BidResponse placeBid(UUID bidderId, UUID auctionId, PlaceBidRequest req) {
        Auction auction =
                auctionRepository
                        .findById(auctionId)
                        .orElseThrow(() -> new AuctionNotFoundException(auctionId));

        // INV-A7: verify bidder is not BANNED or SUSPENDED
        BidderRef bidderRef = bidValidationService.validateBidder(bidderId);
        if (!bidderRef.isActive()) {
            throw new IllegalAuctionStateException(
                    "Bidder account is not eligible to place bids (INV-A7)");
        }

        UUID previousLeaderId = auction.highestBid().map(Bid::getBidderId).orElse(null);
        Bid bid = auction.placeBid(bidderId, Money.of(req.amount()));
        auctionRepository.save(auction);

        if (previousLeaderId != null && !previousLeaderId.equals(bidderId)) {
            notificationClient.sendAsync(
                    previousLeaderId,
                    "BID_OUTBID",
                    Map.of("auctionId", auctionId.toString()));
        }
        return BidResponse.from(bid);
    }

    public AuctionResponse buyNow(UUID buyerId, UUID auctionId) {
        Auction auction =
                auctionRepository
                        .findById(auctionId)
                        .orElseThrow(() -> new AuctionNotFoundException(auctionId));

        UUID sellerId = auction.getSellerId();
        auction.buyNow(buyerId);
        AuctionResponse response = AuctionResponse.from(auctionRepository.save(auction));
        deliveryClient.createJobAsync(
                auctionId, sellerId, buyerId,
                auction.getBuyNowPrice().getAmount());
        notificationClient.sendAsync(
                sellerId, "AUCTION_ENDED_SELLER", Map.of("auctionId", auctionId.toString()));
        notificationClient.sendAsync(
                buyerId, "AUCTION_WON", Map.of("auctionId", auctionId.toString()));
        return response;
    }

    public AuctionResponse cancelAuction(UUID sellerId, UUID auctionId) {
        Auction auction =
                auctionRepository
                        .findById(auctionId)
                        .orElseThrow(() -> new AuctionNotFoundException(auctionId));

        if (!auction.getSellerId().equals(sellerId)) {
            throw new IllegalStateException("Only the seller can cancel this auction");
        }

        auction.cancel();
        return AuctionResponse.from(auctionRepository.save(auction));
    }

    public void removeAuction(UUID auctionId) {
        Auction auction =
                auctionRepository
                        .findById(auctionId)
                        .orElseThrow(() -> new AuctionNotFoundException(auctionId));
        auction.markRemoved();
        auctionRepository.save(auction);
    }

    @Transactional(readOnly = true)
    public List<BidResponse> getBidHistory(UUID auctionId) {
        Auction auction =
                auctionRepository
                        .findById(auctionId)
                        .orElseThrow(() -> new AuctionNotFoundException(auctionId));
        return auction.getBids().stream()
                .sorted((a, b) -> b.getPlacedAt().compareTo(a.getPlacedAt()))
                .map(BidResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuctionResponse> getMyBids(UUID bidderId) {
        return auctionRepository.findByBidderId(bidderId).stream()
                .map(AuctionResponse::from)
                .toList();
    }
}
