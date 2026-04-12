package com.bidhub.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bidhub.admin.application.dto.CategoryResponse;
import com.bidhub.admin.application.dto.CreateCategoryRequest;
import com.bidhub.admin.application.dto.UpdateCategoryRequest;
import com.bidhub.admin.application.service.CategoryManagementService;
import com.bidhub.admin.domain.exception.CategoryNotFoundException;
import com.bidhub.admin.domain.model.Category;
import com.bidhub.admin.domain.repository.CategoryRepository;
import com.bidhub.admin.infrastructure.acl.CatalogueClient;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryManagementServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private CatalogueClient catalogueClient;
    @InjectMocks private CategoryManagementService categoryManagementService;

    @Test
    @DisplayName("createCategory saves and returns CategoryResponse")
    void createCategory_happyPath() {
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

        CreateCategoryRequest req = new CreateCategoryRequest("Electronics", "Electronic goods", null);
        CategoryResponse response = categoryManagementService.createCategory(req);

        assertThat(response.name()).isEqualTo("Electronics");
        assertThat(response.isActive()).isTrue();
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("getCategory throws CategoryNotFoundException when not found")
    void getCategory_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> categoryManagementService.getCategory(id))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    @DisplayName("getCategory returns response when found")
    void getCategory_found_returns() {
        Category cat = Category.create("Books", "Books category", null);
        when(categoryRepository.findById(cat.getCategoryId())).thenReturn(Optional.of(cat));

        CategoryResponse response = categoryManagementService.getCategory(cat.getCategoryId());
        assertThat(response.name()).isEqualTo("Books");
    }

    @Test
    @DisplayName("updateCategory renames and saves")
    void updateCategory_renamesAndSaves() {
        Category cat = Category.create("Old", "desc", null);
        when(categoryRepository.findById(cat.getCategoryId())).thenReturn(Optional.of(cat));
        when(categoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UpdateCategoryRequest req = new UpdateCategoryRequest("New Name", "new desc", null);
        CategoryResponse response = categoryManagementService.updateCategory(cat.getCategoryId(), req);

        assertThat(response.name()).isEqualTo("New Name");
    }

    @Test
    @DisplayName("deactivateCategory deactivates when no active listings (INV-2 passes)")
    void deactivateCategory_noActiveListings_deactivates() {
        Category cat = Category.create("Tools", "desc", null);
        when(categoryRepository.findById(cat.getCategoryId())).thenReturn(Optional.of(cat));
        when(categoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(catalogueClient.countActiveListings(cat.getCategoryId())).thenReturn(0L);

        CategoryResponse response = categoryManagementService.deactivateCategory(cat.getCategoryId());
        assertThat(response.isActive()).isFalse();
    }

    @Test
    @DisplayName("INV-2: deactivateCategory throws when catalogue-service is unreachable (fail-closed)")
    void deactivateCategory_catalogueDown_throws() {
        Category cat = Category.create("Tools", "desc", null);
        when(categoryRepository.findById(cat.getCategoryId())).thenReturn(Optional.of(cat));
        when(catalogueClient.countActiveListings(cat.getCategoryId())).thenReturn(-1L);

        assertThatThrownBy(() -> categoryManagementService.deactivateCategory(cat.getCategoryId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("INV-2");
    }
}
