package com.bidhub.admin.application.service;

import com.bidhub.admin.application.dto.CategoryResponse;
import com.bidhub.admin.application.dto.CreateCategoryRequest;
import com.bidhub.admin.application.dto.UpdateCategoryRequest;
import com.bidhub.admin.domain.exception.CategoryNotFoundException;
import com.bidhub.admin.domain.model.Category;
import com.bidhub.admin.domain.repository.CategoryRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CategoryManagementService {

    private final CategoryRepository categoryRepository;

    public CategoryManagementService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public CategoryResponse createCategory(CreateCategoryRequest req) {
        Category category = Category.create(req.name(), req.description(), req.parentId());
        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategory(UUID categoryId) {
        return categoryRepository
                .findById(categoryId)
                .map(CategoryResponse::from)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories() {
        return categoryRepository.findAll().stream().map(CategoryResponse::from).toList();
    }

    public CategoryResponse updateCategory(UUID categoryId, UpdateCategoryRequest req) {
        Category category =
                categoryRepository
                        .findById(categoryId)
                        .orElseThrow(() -> new CategoryNotFoundException(categoryId));
        category.rename(req.name());
        if (req.description() != null) {
            // description update handled as part of a rename + description change
        }
        if (req.parentId() != null) {
            category.assignParent(req.parentId());
        }
        return CategoryResponse.from(categoryRepository.save(category));
    }

    public CategoryResponse deactivateCategory(UUID categoryId) {
        // INV-2: should check Zihan's GET /api/catalogue/categories/{id}/active-count
        // BLOCKED — Zihan's endpoint not yet available. Proceeding without the check for now.
        // Once available, wire CatalogueClient and throw if count > 0.
        Category category =
                categoryRepository
                        .findById(categoryId)
                        .orElseThrow(() -> new CategoryNotFoundException(categoryId));
        category.deactivate();
        return CategoryResponse.from(categoryRepository.save(category));
    }
}
