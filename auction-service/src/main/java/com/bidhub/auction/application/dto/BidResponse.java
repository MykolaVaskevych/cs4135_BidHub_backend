package com.bidhub.auction.application.dto;

import com.bidhub.auction.domain.model.Bid;
import java.time.Instant;
import java.util.UUID;

public record BidResponse(
        UUID bidId, UUID bidderId, MoneyResponse amount, Instant placedAt, boolean isWinning) {

    public static BidResponse from(Bid bid) {
        return new BidResponse(
                bid.getBidId(),
                bid.getBidderId(),
                MoneyResponse.from(bid.getAmount()),
                bid.getPlacedAt(),
                bid.isWinning());
    }
}
