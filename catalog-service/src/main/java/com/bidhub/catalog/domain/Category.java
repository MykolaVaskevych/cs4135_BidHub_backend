package com.bidhub.catalog.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    private UUID categoryId;

    private String name;
    private UUID parentId;
    private int depth;
    private String slug;
    private boolean isActive;
    private LocalDateTime lastIndexedAt;

    public static Category fromCategoryCreated(UUID categoryId, String name,
            UUID parentId, int depth, String slug) {
        Category c = new Category();
        c.categoryId = categoryId;
        c.name = name;
        c.parentId = parentId;
        c.depth = depth;
        c.slug = slug;
        c.isActive = true;
        c.lastIndexedAt = LocalDateTime.now();
        return c;
    }

    public void applyCategoryUpdated(String name, String slug) {
        this.name = name;
        this.slug = slug;
        this.lastIndexedAt = LocalDateTime.now();
    }

    public void applyCategoryDeleted() {
        this.isActive = false;
        this.lastIndexedAt = LocalDateTime.now();
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public String getName() {
        return name;
    }

    public UUID getParentId() {
        return parentId;
    }

    public int getDepth() {
        return depth;
    }

    public String getSlug() {
        return slug;
    }

    public boolean isActive() {
        return isActive;
    }

    public LocalDateTime getLastIndexedAt() {
        return lastIndexedAt;
    }
}