package com.bidhub.admin.domain.repository;

import com.bidhub.admin.domain.model.Category;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * INV-1: No delete method. Deactivation is performed via Category.deactivate() + save().
 */
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByIsActiveTrue();

    List<Category> findByParentId(UUID parentId);
}
