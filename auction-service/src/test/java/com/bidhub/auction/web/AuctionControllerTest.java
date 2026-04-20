package com.bidhub.auction.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bidhub.auction.application.dto.AuctionResponse;
import com.bidhub.auction.application.dto.BidResponse;
import com.bidhub.auction.application.dto.CreateAuctionRequest;
import com.bidhub.auction.application.dto.MoneyResponse;
import com.bidhub.auction.application.dto.PlaceBidRequest;
import com.bidhub.auction.application.service.AuctionService;
import com.bidhub.auction.domain.exception.AuctionNotFoundException;
import com.bidhub.auction.domain.exception.IllegalAuctionStateException;
import com.bidhub.auction.domain.model.AuctionStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = com.bidhub.auction.web.controller.AuctionController.class)
class AuctionControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private AuctionService auctionService;

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final UUID BUYER_ID = UUID.randomUUID();
    private static final UUID AUCTION_ID = UUID.randomUUID();
    private static final UUID LISTING_ID = UUID.randomUUID();

    private AuctionResponse sampleAuctionResponse() {
        MoneyResponse price = new MoneyResponse(BigDecimal.TEN, "EUR");
        return new AuctionResponse(
                AUCTION_ID,
                LISTING_ID,
                SELLER_ID,
                price,
                new MoneyResponse(BigDecimal.valueOf(20), "EUR"),
                null,
                price,
                AuctionStatus.ACTIVE,
                Instant.now().plusSeconds(3600),
                0,
                null,
                Instant.now());
    }

    @Test
    @DisplayName("POST /api/auctions → 201 Created")
    void createAuction_returns201() throws Exception {
        when(auctionService.createAuction(any(UUID.class), any(CreateAuctionRequest.class)))
                .thenReturn(sampleAuctionResponse());

        CreateAuctionRequest req =
                new CreateAuctionRequest(
                        LISTING_ID,
                        BigDecimal.TEN,
                        BigDecimal.valueOf(20),
                        null,
                        Instant.now().plusSeconds(3600));

        mockMvc.perform(
                        post("/api/auctions")
                                .header("X-User-Id", SELLER_ID.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.auctionId").value(AUCTION_ID.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("GET /api/auctions/{id} → 200 OK")
    void getAuction_returns200() throws Exception {
        when(auctionService.getAuction(AUCTION_ID)).thenReturn(sampleAuctionResponse());

        mockMvc.perform(get("/api/auctions/" + AUCTION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auctionId").value(AUCTION_ID.toString()));
    }

    @Test
    @DisplayName("GET /api/auctions/{id} → 404 when not found")
    void getAuction_notFound_returns404() throws Exception {
        when(auctionService.getAuction(AUCTION_ID))
                .thenThrow(new AuctionNotFoundException(AUCTION_ID));

        mockMvc.perform(get("/api/auctions/" + AUCTION_ID)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/auctions/{id}/cancel → 200 OK")
    void cancelAuction_returns200() throws Exception {
        AuctionResponse cancelled =
                new AuctionResponse(
                        AUCTION_ID,
                        LISTING_ID,
                        SELLER_ID,
                        new MoneyResponse(BigDecimal.TEN, "EUR"),
                        new MoneyResponse(BigDecimal.valueOf(20), "EUR"),
                        null,
                        new MoneyResponse(BigDecimal.TEN, "EUR"),
                        AuctionStatus.CANCELLED,
                        Instant.now().plusSeconds(3600),
                        0,
                        null,
                        Instant.now());
        when(auctionService.cancelAuction(eq(SELLER_ID), eq(AUCTION_ID))).thenReturn(cancelled);

        mockMvc.perform(
                        post("/api/auctions/" + AUCTION_ID + "/cancel")
                                .header("X-User-Id", SELLER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("POST /api/auctions/{id}/cancel → 409 when auction has bids")
    void cancelAuction_hasBids_returns409() throws Exception {
        when(auctionService.cancelAuction(any(UUID.class), eq(AUCTION_ID)))
                .thenThrow(
                        new IllegalAuctionStateException(
                                "Cannot cancel auction with bids"));

        mockMvc.perform(
                        post("/api/auctions/" + AUCTION_ID + "/cancel")
                                .header("X-User-Id", SELLER_ID.toString()))
                .andExpect(status().isConflict());
    }
}
