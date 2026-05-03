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
import com.bidhub.auction.domain.model.AuctionStatus;
import com.bidhub.auction.domain.model.Bid;
import com.bidhub.auction.domain.model.BidderRef;
import com.bidhub.auction.domain.model.Listing;
import com.bidhub.auction.domain.model.Money;
import com.bidhub.auction.domain.repository.AuctionRepository;
import com.bidhub.auction.domain.repository.BidRepository;
import com.bidhub.auction.domain.repository.ListingRepository;
import com.bidhub.auction.domain.service.BidValidationService;
import com.bidhub.auction.infrastructure.acl.AccountClient;
import com.bidhub.auction.infrastructure.acl.AddressInfo;
import com.bidhub.auction.infrastructure.acl.CatalogueClient;
import com.bidhub.auction.infrastructure.acl.DeliveryClient;
import com.bidhub.auction.infrastructure.acl.NotificationClient;
import com.bidhub.auction.infrastructure.acl.OrderClient;
import com.bidhub.auction.infrastructure.acl.PaymentClient;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@Transactional
public class AuctionService {

    private static final Logger log = LoggerFactory.getLogger(AuctionService.class);

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final ListingRepository listingRepository;
    private final BidValidationService bidValidationService;
    private final AccountClient accountClient;
    private final PaymentClient paymentClient;
    private final OrderClient orderClient;
    private final DeliveryClient deliveryClient;
    private final NotificationClient notificationClient;
    private final CatalogueClient catalogueClient;

    public AuctionService(
            AuctionRepository auctionRepository,
            BidRepository bidRepository,
            ListingRepository listingRepository,
            BidValidationService bidValidationService,
            AccountClient accountClient,
            PaymentClient paymentClient,
            OrderClient orderClient,
            DeliveryClient deliveryClient,
            NotificationClient notificationClient,
            CatalogueClient catalogueClient) {
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
        this.listingRepository = listingRepository;
        this.bidValidationService = bidValidationService;
        this.accountClient = accountClient;
        this.paymentClient = paymentClient;
        this.orderClient = orderClient;
        this.deliveryClient = deliveryClient;
        this.notificationClient = notificationClient;
        this.catalogueClient = catalogueClient;
    }

    public AuctionResponse createAuction(UUID sellerId, CreateAuctionRequest req) {
        Listing listing = listingRepository
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

        AuctionResponse response = AuctionResponse.from(auctionRepository.save(auction));
        catalogueClient.indexListingAsync(
                listing.getListingId(), listing.getTitle(), listing.getCategoryId(),
                sellerId, req.startingPrice(), req.endTime());
        return response;
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
        catalogueClient.updatePriceAsync(auction.getListingId(), bid.getAmount().getAmount());

        if (previousLeaderId != null && !previousLeaderId.equals(bidderId)) {
            final UUID outbidId = previousLeaderId;
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notificationClient.sendAsync(
                            outbidId, "BID_OUTBID", Map.of("auctionId", auctionId.toString()));
                }
            });
        }
        return BidResponse.from(bid);
    }

    public AuctionResponse buyNow(UUID buyerId, UUID auctionId) {
        Auction auction =
                auctionRepository
                        .findById(auctionId)
                        .orElseThrow(() -> new AuctionNotFoundException(auctionId));

        UUID sellerId = auction.getSellerId();
        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new IllegalAuctionStateException(
                    "Buy-now is only available on ACTIVE auctions; current status: "
                            + auction.getStatus());
        }
        if (auction.getBuyNowPrice() == null) {
            throw new IllegalAuctionStateException("This auction does not have a buy-now price");
        }
        if (buyerId.equals(sellerId)) {
            throw new IllegalAuctionStateException("Seller cannot buy-now their own auction");
        }
        BigDecimal price = auction.getBuyNowPrice().getAmount();

        AddressInfo buyerAddress = accountClient.defaultAddressOf(buyerId, "buyer");
        AddressInfo sellerAddress = accountClient.defaultAddressOf(sellerId, "seller");

        PaymentClient.ChargeResult charge =
                paymentClient.charge(buyerId, price, "buy-now auction " + auctionId);
        UUID transactionId = charge.transactionId();

        UUID orderId = null;
        Auction saved;
        try {
            orderId = orderClient.createOrder(auctionId, buyerId, sellerId, price);
            deliveryClient.createDeliveryJob(
                    orderId,
                    auctionId,
                    sellerId,
                    buyerId,
                    sellerAddress,
                    buyerAddress,
                    transactionId,
                    price);
            auction.buyNow(buyerId);
            saved = auctionRepository.saveAndFlush(auction);
        } catch (RuntimeException ex) {
            if (orderId != null) {
                tryCancelOrder(orderId);
            }
            tryRefund(buyerId, price);
            throw ex;
        }

        AuctionResponse response = AuctionResponse.from(saved);
        final UUID finalOrderId = orderId;
        final UUID listingId = saved.getListingId();
        final String auctionIdStr = auctionId.toString();
        afterCommitOrRunNow(
                () -> {
                    catalogueClient.updateStatusAsync(listingId, "SOLD");
                    Map<String, String> vars =
                            Map.of(
                                    "auctionId", auctionIdStr,
                                    "orderId", finalOrderId.toString());
                    notificationClient.sendAsync(sellerId, "AUCTION_ENDED_SELLER", vars);
                    notificationClient.sendAsync(buyerId, "AUCTION_WON", vars);
                });
        return response;
    }

    private void afterCommitOrRunNow(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            action.run();
                        }
                    });
        } else {
            action.run();
        }
    }

    private void tryCancelOrder(UUID orderId) {
        try {
            orderClient.cancelOrder(orderId);
        } catch (Exception ex) {
            log.error(
                    "Compensation failed: could not cancel orderId={}; manual reconciliation required",
                    orderId,
                    ex);
        }
    }

    private void tryRefund(UUID buyerId, BigDecimal amount) {
        try {
            paymentClient.refund(buyerId, amount);
        } catch (Exception ex) {
            log.error(
                    "Compensation failed: could not refund buyerId={} amount={}; manual reconciliation required",
                    buyerId,
                    amount,
                    ex);
        }
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
        AuctionResponse response = AuctionResponse.from(auctionRepository.save(auction));
        catalogueClient.updateStatusAsync(auction.getListingId(), "CANCELLED");
        return response;
    }

    public void removeAuction(UUID auctionId) {
        Auction auction =
                auctionRepository
                        .findById(auctionId)
                        .orElseThrow(() -> new AuctionNotFoundException(auctionId));
        auction.markRemoved();
        auctionRepository.save(auction);
        catalogueClient.updateStatusAsync(auction.getListingId(), "REMOVED");
    }

    @Transactional(readOnly = true)
    public List<BidResponse> getBidHistory(UUID auctionId) {
        if (!auctionRepository.existsById(auctionId)) throw new AuctionNotFoundException(auctionId);
        return bidRepository.findByAuction_AuctionIdOrderByPlacedAtDesc(auctionId)
                .stream().map(BidResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public long countByStatus(AuctionStatus status) {
        return auctionRepository.countByStatus(status);
    }

    @Transactional(readOnly = true)
    public List<AuctionResponse> getMyBids(UUID bidderId) {
        return auctionRepository.findByBidderId(bidderId).stream()
                .map(AuctionResponse::from)
                .toList();
    }
}
