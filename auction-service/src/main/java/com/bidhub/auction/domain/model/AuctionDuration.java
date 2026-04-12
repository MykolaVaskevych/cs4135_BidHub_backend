package com.bidhub.auction.domain.model;

import jakarta.persistence.Embeddable;
import java.time.Instant;
import java.util.Objects;

/** Immutable Value Object capturing the temporal bounds of an auction. */
@Embeddable
public final class AuctionDuration {

    private Instant startTime;
    private Instant endTime;

    /** JPA no-arg constructor. */
    protected AuctionDuration() {}

    private AuctionDuration(Instant startTime, Instant endTime) {
        this.startTime = Objects.requireNonNull(startTime, "startTime must not be null");
        this.endTime = Objects.requireNonNull(endTime, "endTime must not be null");
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
    }

    public static AuctionDuration of(Instant startTime, Instant endTime) {
        return new AuctionDuration(startTime, endTime);
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(endTime);
    }

    public long remainingSeconds() {
        return endTime.getEpochSecond() - Instant.now().getEpochSecond();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuctionDuration other)) return false;
        return Objects.equals(startTime, other.startTime)
                && Objects.equals(endTime, other.endTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startTime, endTime);
    }
}
