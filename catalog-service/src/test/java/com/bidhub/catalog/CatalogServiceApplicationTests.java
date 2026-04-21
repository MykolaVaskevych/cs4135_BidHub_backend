package com.bidhub.catalog;

import com.bidhub.catalog.domain.IndexedListing;
import com.bidhub.catalog.repository.CategoryRepository;
import com.bidhub.catalog.repository.IndexedListingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CatalogServiceApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private IndexedListingRepository listingRepository;

    @Test
    void contextLoads() {
    }

    @Test
    void getCategoriesReturnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/catalogue/categories"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void searchReturnsEmptyPage() throws Exception {
        mockMvc.perform(get("/api/catalogue/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void activeCountReturnsZeroForUnknownCategory() throws Exception {
        UUID randomId = UUID.randomUUID();
        mockMvc.perform(get("/api/catalogue/categories/" + randomId + "/active-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeCount").value(0));
    }

    @Test
    void searchOnlyReturnsActiveListings() throws Exception {
        IndexedListing active = IndexedListing.fromAuctionCreated(
                UUID.randomUUID(), "Test iPhone", UUID.randomUUID(),
                UUID.randomUUID(), new BigDecimal("100.00"),
                LocalDateTime.now().plusDays(1));
        listingRepository.save(active);

        mockMvc.perform(get("/api/catalogue/search?q=iPhone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    // ── Internal write endpoints ──────────────────────────────────────────────

    @Test
    void indexListingCreatesSearchableEntry() throws Exception {
        UUID listingId = UUID.randomUUID();
        String body = """
                {
                  "listingId": "%s",
                  "title": "MacBook Pro M3",
                  "categoryId": "%s",
                  "sellerId": "%s",
                  "startingPrice": 1200.00,
                  "endTime": "%s"
                }
                """.formatted(listingId, UUID.randomUUID(), UUID.randomUUID(),
                Instant.now().plusSeconds(3600));

        mockMvc.perform(post("/api/catalogue/internal/listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        // listing should now appear in search results
        mockMvc.perform(get("/api/catalogue/search?q=MacBook"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].listingId").value(listingId.toString()));
    }

    @Test
    void updatePriceReflectsInSearch() throws Exception {
        UUID listingId = UUID.randomUUID();
        IndexedListing listing = IndexedListing.fromAuctionCreated(
                listingId, "Vintage Watch", UUID.randomUUID(),
                UUID.randomUUID(), new BigDecimal("200.00"),
                LocalDateTime.now().plusDays(2));
        listingRepository.save(listing);

        mockMvc.perform(put("/api/catalogue/internal/listings/{id}/price", listingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPrice\": 350.00}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/catalogue/search?q=Vintage&minPrice=300"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].currentPrice").value(350.00));
    }

    @Test
    void updateStatusHidesListingFromSearch() throws Exception {
        UUID listingId = UUID.randomUUID();
        IndexedListing listing = IndexedListing.fromAuctionCreated(
                listingId, "Rare Coin", UUID.randomUUID(),
                UUID.randomUUID(), new BigDecimal("50.00"),
                LocalDateTime.now().plusDays(1));
        listingRepository.save(listing);

        mockMvc.perform(put("/api/catalogue/internal/listings/{id}/status", listingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"SOLD\"}"))
                .andExpect(status().isOk());

        // SOLD listing must not appear in search (only ACTIVE shown)
        mockMvc.perform(get("/api/catalogue/search?q=Rare Coin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void syncCategoryAppearsInCategoryList() throws Exception {
        UUID categoryId = UUID.randomUUID();
        String body = """
                {
                  "categoryId": "%s",
                  "name": "Electronics",
                  "parentId": null
                }
                """.formatted(categoryId);

        mockMvc.perform(post("/api/catalogue/internal/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/catalogue/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.categoryId == '%s')]".formatted(categoryId)).exists());
    }

    @Test
    void deactivateCategoryRemovesItFromList() throws Exception {
        UUID categoryId = UUID.randomUUID();
        // first create it
        mockMvc.perform(post("/api/catalogue/internal/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":\"%s\",\"name\":\"ToRemove\",\"parentId\":null}"
                                .formatted(categoryId)))
                .andExpect(status().isOk());

        // then deactivate
        mockMvc.perform(delete("/api/catalogue/internal/categories/{id}", categoryId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/catalogue/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.categoryId == '%s')]".formatted(categoryId)).doesNotExist());
    }
}