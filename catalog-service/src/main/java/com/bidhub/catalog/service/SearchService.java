package com.bidhub.catalog.service;

import com.bidhub.catalog.domain.IndexedListing;
import com.bidhub.catalog.domain.ListingStatus;
import com.bidhub.catalog.repository.IndexedListingRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class SearchService {

    private final IndexedListingRepository listingRepository;

    public SearchService(IndexedListingRepository listingRepository) {
        this.listingRepository = listingRepository;
    }

    public Page<IndexedListing> search(String keyword, UUID categoryId,
            BigDecimal minPrice, BigDecimal maxPrice,
            Pageable pageable) {
        return listingRepository.search(keyword, categoryId, minPrice, maxPrice, pageable);
    }

    @CircuitBreaker(name = "catalogueService", fallbackMethod = "countActiveInCategoryFallback")
    public long countActiveInCategory(UUID categoryId) {
        return listingRepository.countByCategoryIdAndStatus(
                categoryId, ListingStatus.ACTIVE);
    }

    public long countActiveInCategoryFallback(UUID categoryId, Exception e) {
        return -1L;
    }
}