package com.bidhub.admin.web.controller;

import com.bidhub.admin.application.dto.CategoryResponse;
import com.bidhub.admin.application.service.CategoryManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Public Categories", description = "Category listing for all authenticated users")
public class PublicCategoryController {

    private final CategoryManagementService categoryManagementService;

    public PublicCategoryController(CategoryManagementService categoryManagementService) {
        this.categoryManagementService = categoryManagementService;
    }

    @GetMapping
    @Operation(summary = "List active categories", description = "Returns all active categories. Available to any authenticated user.")
    public List<CategoryResponse> listCategories() {
        return categoryManagementService.listActiveCategories();
    }
}
