package com.bidhub.admin.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bidhub.admin.domain.model.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CategoryInvariantTest {

    @Test
    @DisplayName("INV-1: Category.create produces active category with no delete method")
    void create_producesActiveCategory() {
        Category cat = Category.create("Electronics", "Electronic goods", null);
        assertThat(cat.getName()).isEqualTo("Electronics");
        assertThat(cat.isActive()).isTrue();
        assertThat(cat.getParentId()).isNull();
    }

    @Test
    @DisplayName("INV-1: Category has no hard delete — deactivate sets isActive=false")
    void deactivate_setsInactive() {
        Category cat = Category.create("Electronics", "Electronic goods", null);
        cat.deactivate();
        assertThat(cat.isActive()).isFalse();
    }

    @Test
    @DisplayName("INV-1: Already inactive category can be deactivated again without error")
    void deactivate_idempotent() {
        Category cat = Category.create("Electronics", "Desc", null);
        cat.deactivate();
        cat.deactivate(); // should not throw
        assertThat(cat.isActive()).isFalse();
    }

    @Test
    @DisplayName("INV-3: Category name must not be blank")
    void create_blankName_throws() {
        assertThatThrownBy(() -> Category.create("", "desc", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("rename updates name and validates non-blank")
    void rename_updatesName() {
        Category cat = Category.create("Old", "desc", null);
        cat.rename("New Name");
        assertThat(cat.getName()).isEqualTo("New Name");
    }

    @Test
    @DisplayName("rename rejects blank name")
    void rename_blank_throws() {
        Category cat = Category.create("Old", "desc", null);
        assertThatThrownBy(() -> cat.rename(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("activate sets category back to active")
    void activate_setsActive() {
        Category cat = Category.create("Electronics", "desc", null);
        cat.deactivate();
        cat.activate();
        assertThat(cat.isActive()).isTrue();
    }

    @Test
    @DisplayName("assignParent sets parentId")
    void assignParent_setsParentId() {
        java.util.UUID parentId = java.util.UUID.randomUUID();
        Category cat = Category.create("Sub", "desc", null);
        cat.assignParent(parentId);
        assertThat(cat.getParentId()).isEqualTo(parentId);
    }
}
