package com.bidhub.auction.application.service;

import com.bidhub.auction.application.dto.CreateListingRequest;
import com.bidhub.auction.application.dto.ListingResponse;
import com.bidhub.auction.application.dto.UpdateListingRequest;
import com.bidhub.auction.domain.exception.ListingNotFoundException;
import com.bidhub.auction.domain.model.Listing;
import com.bidhub.auction.domain.repository.AuctionRepository;
import com.bidhub.auction.domain.repository.ListingRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ListingService {

    private final ListingRepository listingRepository;
    private final AuctionRepository auctionRepository;

    public ListingService(ListingRepository listingRepository, AuctionRepository auctionRepository) {
        this.listingRepository = listingRepository;
        this.auctionRepository = auctionRepository;
    }

    public ListingResponse createListing(UUID sellerId, CreateListingRequest req) {
        Listing listing =
                Listing.create(sellerId, req.title(), req.description(), req.photos(), req.categoryId());
        return ListingResponse.from(listingRepository.save(listing));
    }

    @Transactional(readOnly = true)
    public ListingResponse getListing(UUID listingId) {
        return listingRepository
                .findById(listingId)
                .map(ListingResponse::from)
                .orElseThrow(() -> new ListingNotFoundException(listingId));
    }

    public ListingResponse updateListing(UUID listingId, UpdateListingRequest req) {
        Listing listing =
                listingRepository
                        .findById(listingId)
                        .orElseThrow(() -> new ListingNotFoundException(listingId));

        // INV-L2: reject update if associated auction already has bids
        auctionRepository
                .findByListingId(listingId)
                .ifPresent(
                        auction -> {
                            if (auction.bidCount() > 0) {
                                throw new IllegalStateException(
                                        "Cannot update listing: associated auction already has bids (INV-L2)");
                            }
                        });

        listing.update(req.title(), req.description(), req.photos(), req.categoryId());
        return ListingResponse.from(listingRepository.save(listing));
    }

    public void deactivateListing(UUID listingId) {
        Listing listing =
                listingRepository
                        .findById(listingId)
                        .orElseThrow(() -> new ListingNotFoundException(listingId));
        listing.deactivate();
        listingRepository.save(listing);
    }
}
