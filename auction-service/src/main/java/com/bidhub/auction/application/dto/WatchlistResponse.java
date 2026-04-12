package com.bidhub.auction.application.dto;

import com.bidhub.auction.domain.model.Watchlist;
import java.util.Set;
import java.util.UUID;

public record WatchlistResponse(UUID watchlistId, UUID userId, Set<UUID> watchedAuctionIds) {

    public static WatchlistResponse from(Watchlist watchlist) {
        return new WatchlistResponse(
                watchlist.getWatchlistId(),
                watchlist.getUserId(),
                watchlist.getWatchedAuctionIds());
    }
}
