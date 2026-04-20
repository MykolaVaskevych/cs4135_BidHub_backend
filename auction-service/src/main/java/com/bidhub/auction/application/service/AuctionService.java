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
import java.time.Instant;
import java.util.List;
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

    public AuctionService(
            AuctionRepository auctionRepository,
            ListingRepository listingRepository,
            BidValidationService bidValidationService,
            DeliveryClient deliveryClient) {
        this.auctionRepository = auctionRepository;
        this.listingRepository = listingRepository;
        this.bidValidationService = bidValidationService;
        this.deliveryClient = deliveryClient;
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

        Bid bid = auction.placeBid(bidderId, Money.of(req.amount()));
        auctionRepository.save(auction);
        return BidResponse.from(bid);
    }

    public AuctionResponse buyNow(UUID buyerId, UUID auctionId) {
        Auction auction =
                auctionRepository
                        .findById(auctionId)
                        .orElseThrow(() -> new AuctionNotFoundException(auctionId));

        auction.buyNow(buyerId);
        AuctionResponse response = AuctionResponse.from(auctionRepository.save(auction));
        deliveryClient.createJobAsync(
                auctionId, auction.getSellerId(), buyerId,
                auction.getBuyNowPrice().getAmount());
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
}
