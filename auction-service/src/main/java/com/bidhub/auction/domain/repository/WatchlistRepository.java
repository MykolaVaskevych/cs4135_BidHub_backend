package com.bidhub.auction.domain.repository;

import com.bidhub.auction.domain.model.Watchlist;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WatchlistRepository extends JpaRepository<Watchlist, UUID> {

    Optional<Watchlist> findByUserId(UUID userId);
}
