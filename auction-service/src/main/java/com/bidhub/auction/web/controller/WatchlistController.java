package com.bidhub.auction.web.controller;

import com.bidhub.auction.application.dto.WatchlistResponse;
import com.bidhub.auction.application.service.WatchlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auctions/watchlists")
@Tag(name = "Watchlist", description = "Manage auction watchlist")
public class WatchlistController {

    private final WatchlistService watchlistService;

    public WatchlistController(WatchlistService watchlistService) {
        this.watchlistService = watchlistService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get my watchlist")
    @ApiResponse(responseCode = "200", description = "Watchlist returned")
    public WatchlistResponse getWatchlist(@RequestHeader("X-User-Id") UUID userId) {
        return watchlistService.getWatchlist(userId);
    }

    @PostMapping("/me/{auctionId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add auction to watchlist", description = "Adds an ACTIVE auction to the user's watchlist (INV-W1, W2).")
    @ApiResponse(responseCode = "201", description = "Added to watchlist")
    @ApiResponse(responseCode = "400", description = "Auction already in watchlist")
    @ApiResponse(responseCode = "409", description = "Auction is not ACTIVE")
    public WatchlistResponse addToWatchlist(
            @RequestHeader("X-User-Id") UUID userId, @PathVariable UUID auctionId) {
        return watchlistService.addToWatchlist(userId, auctionId);
    }

    @DeleteMapping("/me/{auctionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove auction from watchlist")
    @ApiResponse(responseCode = "204", description = "Removed from watchlist")
    public void removeFromWatchlist(
            @RequestHeader("X-User-Id") UUID userId, @PathVariable UUID auctionId) {
        watchlistService.removeFromWatchlist(userId, auctionId);
    }
}
