package com.bidhub.catalog.controller;

import com.bidhub.catalog.domain.ListingStatus;
import com.bidhub.catalog.dto.IndexListingRequest;
import com.bidhub.catalog.dto.SyncCategoryRequest;
import com.bidhub.catalog.dto.UpdatePriceRequest;
import com.bidhub.catalog.dto.UpdateStatusRequest;
import com.bidhub.catalog.service.IndexingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/catalogue/internal")
public class CatalogueInternalController {

    private final IndexingService indexingService;

    public CatalogueInternalController(IndexingService indexingService) {
        this.indexingService = indexingService;
    }

    @PostMapping("/listings")
    public ResponseEntity<Void> indexListing(@RequestBody IndexListingRequest req) {
        indexingService.indexListing(req.listingId(), req.title(), req.categoryId(),
                req.sellerId(), req.startingPrice(), req.endTime());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/listings/{id}/price")
    public ResponseEntity<Void> updatePrice(@PathVariable UUID id,
            @RequestBody UpdatePriceRequest req) {
        indexingService.updateListingPrice(id, req.newPrice());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/listings/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable UUID id,
            @RequestBody UpdateStatusRequest req) {
        indexingService.updateListingStatus(id, ListingStatus.valueOf(req.status()));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/categories")
    public ResponseEntity<Void> syncCategory(@RequestBody SyncCategoryRequest req) {
        indexingService.syncCategory(req.categoryId(), req.name(), req.parentId());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<Void> updateCategory(@PathVariable UUID id,
            @RequestBody SyncCategoryRequest req) {
        indexingService.syncCategory(id, req.name(), req.parentId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deactivateCategory(@PathVariable UUID id) {
        indexingService.deactivateCategory(id);
        return ResponseEntity.ok().build();
    }
}
