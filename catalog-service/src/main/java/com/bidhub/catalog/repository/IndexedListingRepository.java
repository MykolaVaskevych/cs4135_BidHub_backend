package com.bidhub.catalog.repository;

import com.bidhub.catalog.domain.IndexedListing;
import com.bidhub.catalog.domain.ListingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
public interface IndexedListingRepository extends JpaRepository<IndexedListing, UUID> {

    @Query("SELECT l FROM IndexedListing l WHERE " +
            "l.status = 'ACTIVE' AND " +
            "(:keyword IS NULL OR LOWER(l.title) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:categoryId IS NULL OR l.categoryId = :categoryId) AND " +
            "(:minPrice IS NULL OR l.currentPrice >= :minPrice) AND " +
            "(:maxPrice IS NULL OR l.currentPrice <= :maxPrice)")
    Page<IndexedListing> search(
            @Param("keyword") String keyword,
            @Param("categoryId") UUID categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);

    long countByCategoryIdAndStatus(UUID categoryId, ListingStatus status);
}