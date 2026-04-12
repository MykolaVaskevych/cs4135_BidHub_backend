package com.bidhub.auction.application.service;

import com.bidhub.auction.application.dto.WatchlistResponse;
import com.bidhub.auction.domain.exception.AuctionNotFoundException;
import com.bidhub.auction.domain.model.Auction;
import com.bidhub.auction.domain.model.AuctionStatus;
import com.bidhub.auction.domain.model.Watchlist;
import com.bidhub.auction.domain.repository.AuctionRepository;
import com.bidhub.auction.domain.repository.WatchlistRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WatchlistService {

    private final WatchlistRepository watchlistRepository;
    private final AuctionRepository auctionRepository;

    public WatchlistService(
            WatchlistRepository watchlistRepository, AuctionRepository auctionRepository) {
        this.watchlistRepository = watchlistRepository;
        this.auctionRepository = auctionRepository;
    }

    @Transactional(readOnly = true)
    public WatchlistResponse getWatchlist(UUID userId) {
        Watchlist watchlist =
                watchlistRepository
                        .findByUserId(userId)
                        .orElseGet(
                                () -> watchlistRepository.save(Watchlist.create(userId)));
        return WatchlistResponse.from(watchlist);
    }

    public WatchlistResponse addToWatchlist(UUID userId, UUID auctionId) {
        Watchlist watchlist =
                watchlistRepository
                        .findByUserId(userId)
                        .orElseGet(() -> watchlistRepository.save(Watchlist.create(userId)));

        Auction auction =
                auctionRepository
                        .findById(auctionId)
                        .orElseThrow(() -> new AuctionNotFoundException(auctionId));

        // INV-W2: only ACTIVE auctions can be added
        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Cannot watch non-ACTIVE auction (INV-W2); current status: "
                            + auction.getStatus());
        }

        watchlist.addAuction(auctionId);
        return WatchlistResponse.from(watchlistRepository.save(watchlist));
    }

    public WatchlistResponse removeFromWatchlist(UUID userId, UUID auctionId) {
        Watchlist watchlist =
                watchlistRepository
                        .findByUserId(userId)
                        .orElseGet(() -> watchlistRepository.save(Watchlist.create(userId)));

        watchlist.removeAuction(auctionId);
        return WatchlistResponse.from(watchlistRepository.save(watchlist));
    }
}
