package com.bidhub.admin.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Category aggregate root. Represents a product taxonomy node.
 *
 * <p>INV-1: No hard delete — deactivate() sets isActive=false. Repository has no delete(UUID).<br>
 * INV-2: Deactivation refused if active listings exist — enforced at application service layer
 * (cross-service invariant, requires call to catalogue-service).<br>
 * INV-3: name must not be blank.
 */
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @Column(name = "category_id", nullable = false, updatable = false)
    private UUID categoryId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    /** JPA no-arg constructor. */
    protected Category() {}

    public static Category create(String name, String description, UUID parentId) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Category name must not be blank (INV-3)");
        }
        Category cat = new Category();
        cat.categoryId = UUID.randomUUID();
        cat.name = name;
        cat.description = description;
        cat.parentId = parentId;
        cat.isActive = true;
        cat.createdAt = Instant.now();
        cat.updatedAt = cat.createdAt;
        return cat;
    }

    public void rename(String newName) {
        if (newName == null || newName.isBlank()) {
            throw new IllegalArgumentException("Category name must not be blank (INV-3)");
        }
        this.name = newName;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.isActive = false;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.isActive = true;
        this.updatedAt = Instant.now();
    }

    public void updateDescription(String description) {
        this.description = description;
        this.updatedAt = Instant.now();
    }

    public void assignParent(UUID parentId) {
        this.parentId = parentId;
        this.updatedAt = Instant.now();
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public UUID getParentId() {
        return parentId;
    }

    public boolean isActive() {
        return isActive;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
