package com.bidhub.catalog.dto;

import java.util.UUID;

public record SyncCategoryRequest(UUID categoryId, String name, UUID parentId) {}
