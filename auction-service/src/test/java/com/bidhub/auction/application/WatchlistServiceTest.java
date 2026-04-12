package com.bidhub.auction.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.bidhub.auction.application.dto.WatchlistResponse;
import com.bidhub.auction.application.service.WatchlistService;
import com.bidhub.auction.domain.exception.AuctionNotFoundException;
import com.bidhub.auction.domain.model.Auction;
import com.bidhub.auction.domain.model.AuctionDuration;
import com.bidhub.auction.domain.model.AuctionStatus;
import com.bidhub.auction.domain.model.Money;
import com.bidhub.auction.domain.model.Watchlist;
import com.bidhub.auction.domain.repository.AuctionRepository;
import com.bidhub.auction.domain.repository.WatchlistRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WatchlistServiceTest {

    @Mock private WatchlistRepository watchlistRepository;
    @Mock private AuctionRepository auctionRepository;
    @InjectMocks private WatchlistService watchlistService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID AUCTION_ID = UUID.randomUUID();

    private Auction activeAuction() {
        return Auction.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Money.of(BigDecimal.TEN),
                Money.of(BigDecimal.valueOf(20)),
                null,
                AuctionDuration.of(Instant.now(), Instant.now().plusSeconds(3600)));
    }

    @Test
    @DisplayName("getWatchlist creates a new watchlist when user has none")
    void getWatchlist_noExistingWatchlist_createsNew() {
        when(watchlistRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
        when(watchlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        WatchlistResponse response = watchlistService.getWatchlist(USER_ID);

        assertThat(response.userId()).isEqualTo(USER_ID);
        assertThat(response.watchedAuctionIds()).isEmpty();
    }

    @Test
    @DisplayName("getWatchlist returns existing watchlist")
    void getWatchlist_existing_returns() {
        Watchlist wl = Watchlist.create(USER_ID);
        when(watchlistRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wl));

        WatchlistResponse response = watchlistService.getWatchlist(USER_ID);

        assertThat(response.userId()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("addToWatchlist adds ACTIVE auction and saves")
    void addToWatchlist_activeAuction_added() {
        Watchlist wl = Watchlist.create(USER_ID);
        Auction auction = activeAuction();
        when(watchlistRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wl));
        when(auctionRepository.findById(auction.getAuctionId()))
                .thenReturn(Optional.of(auction));
        when(watchlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        WatchlistResponse response =
                watchlistService.addToWatchlist(USER_ID, auction.getAuctionId());

        assertThat(response.watchedAuctionIds()).contains(auction.getAuctionId());
    }

    @Test
    @DisplayName("INV-W2: addToWatchlist throws when auction is not ACTIVE")
    void addToWatchlist_nonActiveAuction_throws() {
        Watchlist wl = Watchlist.create(USER_ID);
        Auction auction = activeAuction();
        auction.cancel(); // now CANCELLED
        when(watchlistRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wl));
        when(auctionRepository.findById(auction.getAuctionId()))
                .thenReturn(Optional.of(auction));

        assertThatThrownBy(() -> watchlistService.addToWatchlist(USER_ID, auction.getAuctionId()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("addToWatchlist throws AuctionNotFoundException when auction does not exist")
    void addToWatchlist_auctionNotFound_throws() {
        Watchlist wl = Watchlist.create(USER_ID);
        UUID missingId = UUID.randomUUID();
        when(watchlistRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wl));
        when(auctionRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> watchlistService.addToWatchlist(USER_ID, missingId))
                .isInstanceOf(AuctionNotFoundException.class);
    }

    @Test
    @DisplayName("removeFromWatchlist removes the auction and saves")
    void removeFromWatchlist_removes() {
        Watchlist wl = Watchlist.create(USER_ID);
        wl.addAuction(AUCTION_ID);
        when(watchlistRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wl));
        when(watchlistRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        WatchlistResponse response = watchlistService.removeFromWatchlist(USER_ID, AUCTION_ID);

        assertThat(response.watchedAuctionIds()).doesNotContain(AUCTION_ID);
    }
}
