package com.bidhub.admin.application.service;

import com.bidhub.admin.application.dto.CategoryResponse;
import com.bidhub.admin.application.dto.CreateCategoryRequest;
import com.bidhub.admin.application.dto.UpdateCategoryRequest;
import com.bidhub.admin.domain.exception.CategoryNotFoundException;
import com.bidhub.admin.domain.model.Category;
import com.bidhub.admin.domain.repository.CategoryRepository;
import com.bidhub.admin.infrastructure.acl.CatalogueClient;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CategoryManagementService {

    private final CategoryRepository categoryRepository;
    private final CatalogueClient catalogueClient;

    public CategoryManagementService(
            CategoryRepository categoryRepository, CatalogueClient catalogueClient) {
        this.categoryRepository = categoryRepository;
        this.catalogueClient = catalogueClient;
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

    @Transactional(readOnly = true)
    public List<CategoryResponse> listActiveCategories() {
        return categoryRepository.findAll().stream()
                .filter(Category::isActive)
                .map(CategoryResponse::from)
                .toList();
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

    public CategoryResponse activateCategory(UUID categoryId) {
        Category category =
                categoryRepository
                        .findById(categoryId)
                        .orElseThrow(() -> new CategoryNotFoundException(categoryId));
        category.activate();
        return CategoryResponse.from(categoryRepository.save(category));
    }

    public void deleteCategory(UUID categoryId) {
        Category category =
                categoryRepository
                        .findById(categoryId)
                        .orElseThrow(() -> new CategoryNotFoundException(categoryId));
        long activeCount = catalogueClient.countActiveListings(categoryId);
        if (activeCount > 0) {
            throw new IllegalStateException(
                    "Cannot delete category: it has " + activeCount + " active listing(s)");
        }
        categoryRepository.delete(category);
    }

    public CategoryResponse deactivateCategory(UUID categoryId) {
        Category category =
                categoryRepository
                        .findById(categoryId)
                        .orElseThrow(() -> new CategoryNotFoundException(categoryId));

        // INV-2: refuse deactivation if active listings exist in this category.
        // CatalogueClient returns -1L when catalogue-service is unreachable (fail-closed).
        long activeCount = catalogueClient.countActiveListings(categoryId);
        if (activeCount > 0) {
            throw new IllegalStateException(
                    "Cannot deactivate category: it has " + activeCount + " active listing(s) (INV-2)");
        }

        category.deactivate();
        return CategoryResponse.from(categoryRepository.save(category));
    }
}
