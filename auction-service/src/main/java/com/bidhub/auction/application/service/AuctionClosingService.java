package com.bidhub.auction.application.service;

import com.bidhub.auction.domain.model.Auction;
import com.bidhub.auction.domain.model.AuctionStatus;
import com.bidhub.auction.domain.repository.AuctionRepository;
import com.bidhub.auction.infrastructure.acl.CatalogueClient;
import com.bidhub.auction.infrastructure.acl.DeliveryClient;
import com.bidhub.auction.infrastructure.acl.NotificationClient;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class AuctionClosingService {

    private static final Logger log = LoggerFactory.getLogger(AuctionClosingService.class);

    private final AuctionRepository auctionRepository;
    private final DeliveryClient deliveryClient;
    private final NotificationClient notificationClient;
    private final CatalogueClient catalogueClient;

    public AuctionClosingService(
            AuctionRepository auctionRepository,
            DeliveryClient deliveryClient,
            NotificationClient notificationClient,
            CatalogueClient catalogueClient) {
        this.auctionRepository = auctionRepository;
        this.deliveryClient = deliveryClient;
        this.notificationClient = notificationClient;
        this.catalogueClient = catalogueClient;
    }

    @Scheduled(fixedDelay = 5000)
    public void closeExpiredAuctions() {
        List<Auction> expired = auctionRepository.findExpiredActive(Instant.now());
        if (!expired.isEmpty()) {
            log.info("Closing {} expired auction(s)", expired.size());
        }
        for (Auction auction : expired) {
            closeOne(auction);
        }
    }

    @Transactional
    public void closeOne(Auction auction) {
        auction.close();
        auctionRepository.save(auction);
        log.info("Auction {} closed → {}", auction.getAuctionId(), auction.getStatus());
        catalogueClient.updateStatusAsync(auction.getListingId(), auction.getStatus().name());

        final String auctionIdStr = auction.getAuctionId().toString();
        final UUID sellerId = auction.getSellerId();
        final boolean sold = auction.getStatus() == AuctionStatus.SOLD;
        final UUID buyerId = sold ? auction.highestBid().get().getBidderId() : null;

        if (sold) {
            deliveryClient.createJobAsync(
                    auction.getAuctionId(),
                    sellerId,
                    buyerId,
                    auction.getCurrentPrice().getAmount());
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notificationClient.sendAsync(
                        sellerId, "AUCTION_ENDED_SELLER", Map.of("auctionId", auctionIdStr));
                if (sold) {
                    notificationClient.sendAsync(
                            buyerId, "AUCTION_WON", Map.of("auctionId", auctionIdStr));
                }
            }
        });
    }
}
