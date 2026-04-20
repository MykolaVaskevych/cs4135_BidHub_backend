package com.bidhub.admin.web.controller;

import com.bidhub.admin.application.dto.CategoryResponse;
import com.bidhub.admin.application.dto.CreateCategoryRequest;
import com.bidhub.admin.application.dto.UpdateCategoryRequest;
import com.bidhub.admin.application.service.CategoryManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/categories")
@Tag(name = "Categories", description = "Admin category management")
public class CategoryController {

    private final CategoryManagementService categoryManagementService;

    public CategoryController(CategoryManagementService categoryManagementService) {
        this.categoryManagementService = categoryManagementService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create category", description = "Creates a new category. Requires ADMIN role.")
    @ApiResponse(responseCode = "201", description = "Category created")
    @ApiResponse(responseCode = "400", description = "Validation error")
    public CategoryResponse createCategory(
            @RequestHeader("X-User-Id") UUID adminId,
            @RequestHeader("X-User-Roles") String roles,
            @Valid @RequestBody CreateCategoryRequest req) {
        return categoryManagementService.createCategory(req);
    }

    @GetMapping
    @Operation(summary = "List all categories")
    @ApiResponse(responseCode = "200", description = "Category list")
    public List<CategoryResponse> listCategories() {
        return categoryManagementService.listCategories();
    }

    @GetMapping("/{categoryId}")
    @Operation(summary = "Get category by ID")
    @ApiResponse(responseCode = "200", description = "Category found")
    @ApiResponse(responseCode = "404", description = "Category not found")
    public CategoryResponse getCategory(@PathVariable UUID categoryId) {
        return categoryManagementService.getCategory(categoryId);
    }

    @PutMapping("/{categoryId}")
    @Operation(summary = "Update category", description = "Updates category name, description, or parent. Requires ADMIN role.")
    @ApiResponse(responseCode = "200", description = "Category updated")
    @ApiResponse(responseCode = "404", description = "Category not found")
    public CategoryResponse updateCategory(
            @RequestHeader("X-User-Id") UUID adminId,
            @RequestHeader("X-User-Roles") String roles,
            @PathVariable UUID categoryId,
            @Valid @RequestBody UpdateCategoryRequest req) {
        return categoryManagementService.updateCategory(categoryId, req);
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deactivate category", description = "Soft-deletes category (INV-1/2). Requires ADMIN role.")
    @ApiResponse(responseCode = "204", description = "Category deactivated")
    @ApiResponse(responseCode = "409", description = "Category has active listings (INV-2)")
    public void deactivateCategory(
            @RequestHeader("X-User-Id") UUID adminId,
            @RequestHeader("X-User-Roles") String roles,
            @PathVariable UUID categoryId) {
        categoryManagementService.deactivateCategory(categoryId);
    }

    @PutMapping("/{categoryId}/activate")
    @Operation(summary = "Reactivate category", description = "Re-enables a deactivated category. Requires ADMIN role.")
    @ApiResponse(responseCode = "200", description = "Category activated")
    public CategoryResponse activateCategory(
            @RequestHeader("X-User-Id") UUID adminId,
            @RequestHeader("X-User-Roles") String roles,
            @PathVariable UUID categoryId) {
        return categoryManagementService.activateCategory(categoryId);
    }

    @DeleteMapping("/{categoryId}/hard")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Hard-delete category", description = "Permanently removes a category. Blocked if it has active listings.")
    @ApiResponse(responseCode = "204", description = "Category deleted")
    @ApiResponse(responseCode = "409", description = "Category has active listings")
    public void hardDeleteCategory(
            @RequestHeader("X-User-Id") UUID adminId,
            @RequestHeader("X-User-Roles") String roles,
            @PathVariable UUID categoryId) {
        categoryManagementService.deleteCategory(categoryId);
    }
}
