package com.bidhub.auction.web.controller;

import com.bidhub.auction.application.dto.AuctionResponse;
import com.bidhub.auction.application.dto.BidResponse;
import com.bidhub.auction.application.dto.CreateAuctionRequest;
import com.bidhub.auction.application.dto.PlaceBidRequest;
import com.bidhub.auction.application.service.AuctionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auctions")
@Tag(name = "Auctions", description = "Manage auctions and bidding")
public class AuctionController {

    private final AuctionService auctionService;

    public AuctionController(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create auction", description = "Creates an auction for a listing. Requires SELLER role.")
    @ApiResponse(responseCode = "201", description = "Auction created")
    @ApiResponse(responseCode = "400", description = "Validation error")
    @ApiResponse(responseCode = "404", description = "Listing not found")
    public AuctionResponse createAuction(
            @RequestHeader("X-User-Id") UUID sellerId,
            @Valid @RequestBody CreateAuctionRequest req) {
        return auctionService.createAuction(sellerId, req);
    }

    @GetMapping("/{auctionId}")
    @Operation(summary = "Get auction by ID")
    @ApiResponse(responseCode = "200", description = "Auction found")
    @ApiResponse(responseCode = "404", description = "Auction not found")
    public AuctionResponse getAuction(@PathVariable UUID auctionId) {
        return auctionService.getAuction(auctionId);
    }

    @GetMapping("/search")
    @Operation(summary = "Search auctions", description = "Search active auctions by keyword and/or category.")
    @ApiResponse(responseCode = "200", description = "Search results")
    public List<AuctionResponse> searchAuctions(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID category) {
        return auctionService.searchAuctions(q, category);
    }

    @PostMapping("/{auctionId}/bids")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Place bid", description = "Places a bid on an auction (INV-A1, A7, A8).")
    @ApiResponse(responseCode = "201", description = "Bid placed")
    @ApiResponse(responseCode = "400", description = "Bid too low or seller bid attempt")
    @ApiResponse(responseCode = "404", description = "Auction not found")
    @ApiResponse(responseCode = "409", description = "Auction not in ACTIVE state or optimistic lock failure")
    public BidResponse placeBid(
            @RequestHeader("X-User-Id") UUID bidderId,
            @PathVariable UUID auctionId,
            @Valid @RequestBody PlaceBidRequest req) {
        return auctionService.placeBid(bidderId, auctionId, req);
    }

    @PostMapping("/{auctionId}/buy-now")
    @Operation(summary = "Execute buy-now", description = "Immediately purchases the auction item at the buy-now price (INV-A3).")
    @ApiResponse(responseCode = "200", description = "Buy-now successful")
    @ApiResponse(responseCode = "409", description = "Auction not ACTIVE or no buy-now price")
    public AuctionResponse buyNow(
            @RequestHeader("X-User-Id") UUID buyerId, @PathVariable UUID auctionId) {
        return auctionService.buyNow(buyerId, auctionId);
    }

    @PostMapping("/{auctionId}/cancel")
    @Operation(summary = "Cancel auction", description = "Cancels the auction. Only allowed when ACTIVE with no bids (INV-A4).")
    @ApiResponse(responseCode = "200", description = "Auction cancelled")
    @ApiResponse(responseCode = "409", description = "Auction has bids or is not ACTIVE")
    public AuctionResponse cancelAuction(
            @RequestHeader("X-User-Id") UUID sellerId, @PathVariable UUID auctionId) {
        return auctionService.cancelAuction(sellerId, auctionId);
    }
}
