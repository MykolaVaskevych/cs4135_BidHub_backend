package com.bidhub.auction.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bidhub.auction.application.dto.CreateListingRequest;
import com.bidhub.auction.application.dto.ListingResponse;
import com.bidhub.auction.application.dto.UpdateListingRequest;
import com.bidhub.auction.application.service.ListingService;
import com.bidhub.auction.domain.exception.ListingNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = com.bidhub.auction.web.controller.ListingController.class)
class ListingControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private ListingService listingService;

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID LISTING_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();

    private ListingResponse sampleResponse() {
        return new ListingResponse(
                LISTING_ID,
                SELLER_ID,
                "Camera",
                "Nice camera",
                List.of("p1.jpg"),
                CATEGORY_ID,
                true,
                Instant.now());
    }

    @Test
    @DisplayName("POST /api/auctions/listings → 201 Created")
    void createListing_returns201() throws Exception {
        when(listingService.createListing(any(UUID.class), any(CreateListingRequest.class)))
                .thenReturn(sampleResponse());

        CreateListingRequest req =
                new CreateListingRequest("Camera", "Nice camera", List.of("p1.jpg"), CATEGORY_ID);

        mockMvc.perform(
                        post("/api/auctions/listings")
                                .header("X-User-Id", SELLER_ID.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.listingId").exists())
                .andExpect(jsonPath("$.title").value("Camera"));
    }

    @Test
    @DisplayName("GET /api/auctions/listings/{id} → 200 OK")
    void getListing_returns200() throws Exception {
        when(listingService.getListing(LISTING_ID)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/auctions/listings/" + LISTING_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.listingId").value(LISTING_ID.toString()));
    }

    @Test
    @DisplayName("GET /api/auctions/listings/{id} → 404 when not found")
    void getListing_notFound_returns404() throws Exception {
        when(listingService.getListing(LISTING_ID))
                .thenThrow(new ListingNotFoundException(LISTING_ID));

        mockMvc.perform(get("/api/auctions/listings/" + LISTING_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/auctions/listings/{id} → 200 OK")
    void updateListing_returns200() throws Exception {
        when(listingService.updateListing(eq(SELLER_ID), eq(LISTING_ID), any(UpdateListingRequest.class)))
                .thenReturn(sampleResponse());

        UpdateListingRequest req =
                new UpdateListingRequest("Camera", "Nice camera", List.of("p1.jpg"), CATEGORY_ID);

        mockMvc.perform(
                        put("/api/auctions/listings/" + LISTING_ID)
                                .header("X-User-Id", SELLER_ID.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/auctions/listings/{id} → 204 No Content")
    void deactivateListing_returns204() throws Exception {
        mockMvc.perform(
                        delete("/api/auctions/listings/" + LISTING_ID)
                                .header("X-User-Id", SELLER_ID.toString()))
                .andExpect(status().isNoContent());
    }
}
