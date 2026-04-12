package com.bidhub.auction.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bidhub.auction.domain.model.Watchlist;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WatchlistInvariantTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID AUCTION_A = UUID.randomUUID();
    private static final UUID AUCTION_B = UUID.randomUUID();

    private Watchlist emptyWatchlist() {
        return Watchlist.create(USER_ID);
    }

    // ---------------------------------------------------------------
    // INV-W1: cannot watch the same auction twice
    // ---------------------------------------------------------------

    @Test
    @DisplayName("INV-W1: adding the same auction twice is rejected")
    void invW1_duplicateAdd_rejected() {
        Watchlist wl = emptyWatchlist();
        wl.addAuction(AUCTION_A);
        assertThatThrownBy(() -> wl.addAuction(AUCTION_A))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("INV-W1: adding distinct auctions is accepted")
    void invW1_distinctAuctions_accepted() {
        Watchlist wl = emptyWatchlist();
        wl.addAuction(AUCTION_A);
        wl.addAuction(AUCTION_B);
        assertThat(wl.isWatching(AUCTION_A)).isTrue();
        assertThat(wl.isWatching(AUCTION_B)).isTrue();
    }

    // ---------------------------------------------------------------
    // INV-W2: pruneClosed removes closed auctions from the watchlist.
    // The "add only ACTIVE" rule is enforced at application service
    // layer (WatchlistService checks auction status before calling
    // addAuction). pruneClosed is the enforcement for removal.
    // ---------------------------------------------------------------

    @Test
    @DisplayName("INV-W2: pruneClosed removes the given auctionId")
    void invW2_pruneClosed_removesAuction() {
        Watchlist wl = emptyWatchlist();
        wl.addAuction(AUCTION_A);
        wl.addAuction(AUCTION_B);
        wl.pruneClosed(AUCTION_A);
        assertThat(wl.isWatching(AUCTION_A)).isFalse();
        assertThat(wl.isWatching(AUCTION_B)).isTrue();
    }

    @Test
    @DisplayName("INV-W2: pruneClosed on non-watched auction is a no-op")
    void invW2_pruneNonWatched_noOp() {
        Watchlist wl = emptyWatchlist();
        wl.addAuction(AUCTION_A);
        wl.pruneClosed(AUCTION_B); // not in watchlist — should not throw
        assertThat(wl.isWatching(AUCTION_A)).isTrue();
    }

    // ---------------------------------------------------------------
    // removeAuction
    // ---------------------------------------------------------------

    @Test
    @DisplayName("removeAuction removes the auction from the watchlist")
    void removeAuction_removesIt() {
        Watchlist wl = emptyWatchlist();
        wl.addAuction(AUCTION_A);
        wl.removeAuction(AUCTION_A);
        assertThat(wl.isWatching(AUCTION_A)).isFalse();
    }

    @Test
    @DisplayName("isWatching returns false for empty watchlist")
    void isWatching_emptyWatchlist_false() {
        assertThat(emptyWatchlist().isWatching(AUCTION_A)).isFalse();
    }
}
