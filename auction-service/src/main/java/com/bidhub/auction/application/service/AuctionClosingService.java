package com.bidhub.auction.application.service;

import com.bidhub.auction.domain.model.Auction;
import com.bidhub.auction.domain.model.AuctionStatus;
import com.bidhub.auction.domain.repository.AuctionRepository;
import com.bidhub.auction.infrastructure.acl.DeliveryClient;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuctionClosingService {

    private static final Logger log = LoggerFactory.getLogger(AuctionClosingService.class);

    private final AuctionRepository auctionRepository;
    private final DeliveryClient deliveryClient;

    public AuctionClosingService(AuctionRepository auctionRepository, DeliveryClient deliveryClient) {
        this.auctionRepository = auctionRepository;
        this.deliveryClient = deliveryClient;
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

        if (auction.getStatus() == AuctionStatus.SOLD) {
            UUID buyerId = auction.highestBid().get().getBidderId();
            deliveryClient.createJobAsync(
                    auction.getAuctionId(),
                    auction.getSellerId(),
                    buyerId,
                    auction.getCurrentPrice().getAmount());
        }
    }
}
