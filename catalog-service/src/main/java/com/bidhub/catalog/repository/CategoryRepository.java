package com.bidhub.catalog.repository;

import com.bidhub.catalog.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByIsActiveTrue();

    List<Category> findByParentId(UUID parentId);

    long countByParentIdAndIsActiveTrue(UUID parentId);
}