package com.bidhub.catalog.service;

import com.bidhub.catalog.domain.Category;
import com.bidhub.catalog.domain.IndexedListing;
import com.bidhub.catalog.domain.ListingStatus;
import com.bidhub.catalog.repository.CategoryRepository;
import com.bidhub.catalog.repository.IndexedListingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
@Transactional
public class IndexingService {

    private final IndexedListingRepository listingRepository;
    private final CategoryRepository categoryRepository;

    public IndexingService(IndexedListingRepository listingRepository,
                           CategoryRepository categoryRepository) {
        this.listingRepository = listingRepository;
        this.categoryRepository = categoryRepository;
    }

    public void indexListing(UUID listingId, String title, UUID categoryId,
                             UUID sellerId, BigDecimal startingPrice, Instant endTime) {
        LocalDateTime endTimeLocal = LocalDateTime.ofInstant(endTime, ZoneId.systemDefault());
        IndexedListing listing = IndexedListing.fromAuctionCreated(
                listingId, title, categoryId, sellerId, startingPrice, endTimeLocal);
        listingRepository.save(listing);
    }

    public void updateListingPrice(UUID listingId, BigDecimal newPrice) {
        listingRepository.findById(listingId).ifPresent(l -> {
            l.applyBidPlaced(newPrice);
            listingRepository.save(l);
        });
    }

    public void updateListingStatus(UUID listingId, ListingStatus status) {
        listingRepository.findById(listingId).ifPresent(l -> {
            l.applyStatusChange(status);
            listingRepository.save(l);
        });
    }

    /**
     * Upsert a category from admin-service. Creates it if missing, updates name/slug if present.
     * slug is derived from name to keep catalog-service self-contained.
     */
    public void syncCategory(UUID categoryId, String name, UUID parentId) {
        String slug = name.toLowerCase().replaceAll("[^a-z0-9]+", "-");
        categoryRepository.findById(categoryId).ifPresentOrElse(
                c -> {
                    c.applyCategoryUpdated(name, slug);
                    categoryRepository.save(c);
                },
                () -> categoryRepository.save(
                        Category.fromCategoryCreated(categoryId, name, parentId, 0, slug)));
    }

    public void deactivateCategory(UUID categoryId) {
        categoryRepository.findById(categoryId).ifPresent(c -> {
            c.applyCategoryDeleted();
            categoryRepository.save(c);
        });
    }
}
