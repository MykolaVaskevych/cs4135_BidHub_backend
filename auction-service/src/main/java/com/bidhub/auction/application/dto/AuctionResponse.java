package com.bidhub.auction.application.dto;

import com.bidhub.auction.domain.model.Auction;
import com.bidhub.auction.domain.model.AuctionStatus;
import java.time.Instant;
import java.util.UUID;

public record AuctionResponse(
        UUID auctionId,
        UUID listingId,
        UUID sellerId,
        MoneyResponse startingPrice,
        MoneyResponse reservePrice,
        MoneyResponse buyNowPrice,
        MoneyResponse currentPrice,
        AuctionStatus status,
        Instant endTime,
        int bidCount,
        UUID leadingBidderId,
        Instant createdAt) {

    public static AuctionResponse from(Auction auction) {
        UUID leadingBidder = auction.highestBid().map(b -> b.getBidderId()).orElse(null);
        return new AuctionResponse(
                auction.getAuctionId(),
                auction.getListingId(),
                auction.getSellerId(),
                MoneyResponse.from(auction.getStartingPrice()),
                MoneyResponse.from(auction.getReservePrice()),
                MoneyResponse.from(auction.getBuyNowPrice()),
                MoneyResponse.from(auction.getCurrentPrice()),
                auction.getStatus(),
                auction.getDuration().getEndTime(),
                auction.bidCount(),
                leadingBidder,
                auction.getCreatedAt());
    }
}
