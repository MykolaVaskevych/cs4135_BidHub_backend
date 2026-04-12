package com.bidhub.admin.application.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record UpdateCategoryRequest(
        @NotBlank String name, String description, UUID parentId) {}
