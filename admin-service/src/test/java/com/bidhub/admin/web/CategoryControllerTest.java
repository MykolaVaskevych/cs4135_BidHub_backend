package com.bidhub.admin.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bidhub.admin.application.dto.CategoryResponse;
import com.bidhub.admin.application.dto.CreateCategoryRequest;
import com.bidhub.admin.application.service.CategoryManagementService;
import com.bidhub.admin.domain.exception.CategoryNotFoundException;
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

@WebMvcTest(controllers = com.bidhub.admin.web.controller.CategoryController.class)
class CategoryControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private CategoryManagementService categoryManagementService;

    private static final UUID ADMIN_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();

    private CategoryResponse sampleResponse() {
        return new CategoryResponse(CATEGORY_ID, "Electronics", "Electronic goods", null, true, Instant.now());
    }

    @Test
    @DisplayName("POST /api/admin/categories → 201 Created")
    void createCategory_returns201() throws Exception {
        when(categoryManagementService.createCategory(any(CreateCategoryRequest.class)))
                .thenReturn(sampleResponse());

        CreateCategoryRequest req = new CreateCategoryRequest("Electronics", "Electronic goods", null);

        mockMvc.perform(
                        post("/api/admin/categories")
                                .header("X-User-Id", ADMIN_ID.toString())
                                .header("X-User-Roles", "ADMIN")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Electronics"));
    }

    @Test
    @DisplayName("GET /api/admin/categories → 200 OK")
    void listCategories_returns200() throws Exception {
        when(categoryManagementService.listCategories()).thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/admin/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Electronics"));
    }

    @Test
    @DisplayName("GET /api/admin/categories/{id} → 404 when not found")
    void getCategory_notFound_returns404() throws Exception {
        when(categoryManagementService.getCategory(CATEGORY_ID))
                .thenThrow(new CategoryNotFoundException(CATEGORY_ID));

        mockMvc.perform(get("/api/admin/categories/" + CATEGORY_ID))
                .andExpect(status().isNotFound());
    }
}
