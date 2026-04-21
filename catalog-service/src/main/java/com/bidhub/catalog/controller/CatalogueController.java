package com.bidhub.catalog.controller;

import com.bidhub.catalog.domain.Category;
import com.bidhub.catalog.domain.IndexedListing;
import com.bidhub.catalog.repository.CategoryRepository;
import com.bidhub.catalog.repository.IndexedListingRepository;
import com.bidhub.catalog.service.SearchService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/catalogue")
public class CatalogueController {

    private final SearchService searchService;
    private final CategoryRepository categoryRepository;
    private final IndexedListingRepository listingRepository;

    public CatalogueController(SearchService searchService,
            CategoryRepository categoryRepository,
            IndexedListingRepository listingRepository) {
        this.searchService = searchService;
        this.categoryRepository = categoryRepository;
        this.listingRepository = listingRepository;
    }

    @GetMapping("/search")
    public Page<IndexedListing> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            Pageable pageable) {
        return searchService.search(q, category, minPrice, maxPrice, pageable);
    }

    @GetMapping("/categories")
    public List<Category> getCategories() {
        return categoryRepository.findByIsActiveTrue();
    }

    @GetMapping("/categories/{id}/active-count")
    public ResponseEntity<Map<String, Long>> getActiveCount(@PathVariable UUID id) {
        long count = searchService.countActiveInCategory(id);
        return ResponseEntity.ok(Map.of("activeCount", count));
    }

    @PostMapping("/listings")
    public ResponseEntity<IndexedListing> addListing(@RequestBody IndexedListing listing) {
        return ResponseEntity.ok(listingRepository.save(listing));
    }

    @PostMapping("/categories")
    public ResponseEntity<Category> addCategory(@RequestBody Category category) {
        return ResponseEntity.ok(categoryRepository.save(category));
    }
}