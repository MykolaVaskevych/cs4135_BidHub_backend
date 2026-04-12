package com.bidhub.auction.domain.repository;

import com.bidhub.auction.domain.model.Listing;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListingRepository extends JpaRepository<Listing, UUID> {

    List<Listing> findBySellerId(UUID sellerId);

    List<Listing> findByCategoryId(UUID categoryId);
}
