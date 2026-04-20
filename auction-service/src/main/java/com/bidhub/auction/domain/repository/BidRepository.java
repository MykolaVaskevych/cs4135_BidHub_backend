package com.bidhub.auction.domain.repository;

import com.bidhub.auction.domain.model.Bid;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidRepository extends JpaRepository<Bid, UUID> {
    List<Bid> findByAuction_AuctionIdOrderByPlacedAtDesc(UUID auctionId);
}
