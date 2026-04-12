package com.bidhub.auction.web.controller;

import com.bidhub.auction.application.dto.CreateListingRequest;
import com.bidhub.auction.application.dto.ListingResponse;
import com.bidhub.auction.application.dto.UpdateListingRequest;
import com.bidhub.auction.application.service.ListingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auctions/listings")
@Tag(name = "Listings", description = "Manage auction listings")
public class ListingController {

    private final ListingService listingService;

    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a listing", description = "Creates a new listing. Requires SELLER role.")
    @ApiResponse(responseCode = "201", description = "Listing created")
    @ApiResponse(responseCode = "400", description = "Validation error")
    public ListingResponse createListing(
            @RequestHeader("X-User-Id") UUID sellerId,
            @Valid @RequestBody CreateListingRequest req) {
        return listingService.createListing(sellerId, req);
    }

    @GetMapping("/{listingId}")
    @Operation(summary = "Get listing by ID")
    @ApiResponse(responseCode = "200", description = "Listing found")
    @ApiResponse(responseCode = "404", description = "Listing not found")
    public ListingResponse getListing(@PathVariable UUID listingId) {
        return listingService.getListing(listingId);
    }

    @PutMapping("/{listingId}")
    @Operation(summary = "Update listing", description = "Updates listing. Fails if associated auction has bids (INV-L2).")
    @ApiResponse(responseCode = "200", description = "Listing updated")
    @ApiResponse(responseCode = "400", description = "Validation error or auction has bids")
    @ApiResponse(responseCode = "404", description = "Listing not found")
    public ListingResponse updateListing(
            @RequestHeader("X-User-Id") UUID sellerId,
            @PathVariable UUID listingId,
            @Valid @RequestBody UpdateListingRequest req) {
        return listingService.updateListing(listingId, req);
    }

    @DeleteMapping("/{listingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate listing", description = "Soft-deletes the listing (INV-L1).")
    @ApiResponse(responseCode = "204", description = "Listing deactivated")
    @ApiResponse(responseCode = "404", description = "Listing not found")
    public void deactivateListing(
            @RequestHeader("X-User-Id") UUID sellerId, @PathVariable UUID listingId) {
        listingService.deactivateListing(listingId);
    }
}
