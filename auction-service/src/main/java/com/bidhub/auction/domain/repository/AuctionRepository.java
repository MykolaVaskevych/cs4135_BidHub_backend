package com.bidhub.auction.domain.repository;

import com.bidhub.auction.domain.model.Auction;
import com.bidhub.auction.domain.model.AuctionStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuctionRepository extends JpaRepository<Auction, UUID> {

    Optional<Auction> findByListingId(UUID listingId);

    List<Auction> findByStatus(AuctionStatus status);

    long countByStatus(AuctionStatus status);

    List<Auction> findBySellerId(UUID sellerId);

    /** Returns all ACTIVE auctions whose endTime has passed with bids eagerly loaded. */
    @Query(
            "SELECT DISTINCT a FROM Auction a LEFT JOIN FETCH a.bids"
                    + " WHERE a.status = 'ACTIVE' AND a.duration.endTime < :now")
    List<Auction> findExpiredActive(@Param("now") Instant now);

    /** Returns auctions where the given user has placed a bid. */
    @Query(
            "SELECT DISTINCT a FROM Auction a JOIN a.bids b WHERE b.bidderId = :bidderId")
    List<Auction> findByBidderId(@Param("bidderId") UUID bidderId);

    /** Simple keyword search on title (case-insensitive). */
    @Query(
            "SELECT a FROM Auction a JOIN Listing l ON a.listingId = l.listingId"
                    + " WHERE a.status = 'ACTIVE'"
                    + " AND (:keyword IS NULL OR LOWER(l.title) LIKE LOWER(CONCAT('%',:keyword,'%')))"
                    + " AND (:categoryId IS NULL OR l.categoryId = :categoryId)")
    List<Auction> searchActive(
            @Param("keyword") String keyword, @Param("categoryId") UUID categoryId);
}
