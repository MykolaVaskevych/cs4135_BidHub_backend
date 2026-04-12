package com.bidhub.admin.application.dto;

import com.bidhub.admin.domain.model.Category;
import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(
        UUID categoryId,
        String name,
        String description,
        UUID parentId,
        boolean isActive,
        Instant createdAt) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getCategoryId(),
                category.getName(),
                category.getDescription(),
                category.getParentId(),
                category.isActive(),
                category.getCreatedAt());
    }
}
