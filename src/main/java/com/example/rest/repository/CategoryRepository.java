package com.example.rest.repository;

import com.example.rest.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    // Find category by name
    Optional<Category> findByName(String name);
    // Use case: Lookup "Electronics" category

    // Find all root categories (top-level, no parent)
    List<Category> findByParentIsNull();
    // SQL: SELECT * FROM categories WHERE parent_id IS NULL
    // Use case: Display main navigation menu

    // Find subcategories of a parent
    List<Category> findByParentId(Long parentId);
    // SQL: SELECT * FROM categories WHERE parent_id = ?
    // Use case: When user clicks "Electronics", show "Laptops", "Phones", etc.

    // Count subcategories
    long countByParentId(Long parentId);
    // Use case: Show "Electronics (5)" indicating 5 subcategories

    // Check if category has subcategories
    boolean existsByParentId(Long parentId);
    // Use case: Prevent deleting categories with subcategories

    // Custom query: Find category with products count
    @Query("SELECT c, COUNT(p) FROM Category c " +
            "LEFT JOIN c.products p " +
            "WHERE c.id = :categoryId " +
            "GROUP BY c")
    Object[] findCategoryWithProductCount(@Param("categoryId") Long categoryId);
    // Use case: Display "Laptops (42 products)"

    // Find categories by name containing keyword (search)
    List<Category> findByNameContainingIgnoreCase(String keyword);
    // Use case: Search categories
}
