package com.example.rest.repository;

import com.example.rest.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    List<Product> findByActiveTrue();

    List<Product> findByCategoryId(Long categoryId);

    // Price range queries
    List<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    // Stock queries
    List<Product> findByStockQuantityLessThan(Integer quantity);

    // Combined conditions
    List<Product> findByActiveTrueAndStockQuantityGreaterThan(Integer quantity);

    // Search in multiple fields
    List<Product> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String name, String description);

    // With pagination and sorting
    Page<Product> findByCategoryIdAndActiveTrue(Long categoryId, Pageable pageable);

    // Fetch with associations
    @Query("SELECT p FROM Product p " +
            "LEFT JOIN FETCH p.category " +
            "LEFT JOIN FETCH p.images " +
            "WHERE p.id = :id")
    Optional<Product> findByIdWithDetails(@Param("id") Long id);

    // Search with category
    @Query("SELECT p FROM Product p " +
            "WHERE p.category.id = :categoryId " +
            "AND p.active = true " +
            "AND p.stockQuantity > 0 " +
            "ORDER BY p.createdAt DESC")
    List<Product> findAvailableProductsByCategory(@Param("categoryId") Long categoryId);

    // Products with average rating
    @Query("SELECT p FROM Product p " +
            "LEFT JOIN p.reviews r " +
            "WHERE p.active = true " +
            "GROUP BY p.id " +
            "HAVING AVG(r.rating) >= :minRating")
    List<Product> findProductsByMinRating(@Param("minRating") Double minRating);

    // Top selling products
    @Query("SELECT p, SUM(oi.quantity) as totalSold FROM Product p " +
            "JOIN OrderItem oi ON oi.product.id = p.id " +
            "JOIN Order o ON oi.order.id = o.id " +
            "WHERE o.status = 'DELIVERED' " +
            "AND o.orderDate >= :startDate " +
            "GROUP BY p.id " +
            "ORDER BY totalSold DESC")
    List<Object[]> findTopSellingProducts(@Param("startDate") LocalDateTime startDate,
                                          Pageable pageable);

    // Low stock alert
    @Query("SELECT p FROM Product p " +
            "WHERE p.active = true " +
            "AND p.stockQuantity <= :threshold " +
            "ORDER BY p.stockQuantity ASC")
    List<Product> findLowStockProducts(@Param("threshold") Integer threshold);

    // Native query for complex PostgreSQL-specific operations
    @Query(value = "SELECT * FROM products p " +
            "WHERE p.active = true " +
            "AND p.price BETWEEN :minPrice AND :maxPrice " +
            "ORDER BY p.created_at DESC " +
            "LIMIT :limit",
            nativeQuery = true)
    List<Product> findProductsNative(@Param("minPrice") BigDecimal minPrice,
                                     @Param("maxPrice") BigDecimal maxPrice,
                                     @Param("limit") int limit);

    // Full-text search (PostgreSQL specific)
    @Query(value = "SELECT * FROM products p " +
            "WHERE to_tsvector('english', p.name || ' ' || p.description) " +
            "@@ to_tsquery('english', :searchTerm)",
            nativeQuery = true)
    List<Product> fullTextSearch(@Param("searchTerm") String searchTerm);

    // Complex aggregation with native SQL
    @Query(value = "SELECT c.name as category_name, " +
            "COUNT(p.id) as product_count, " +
            "AVG(p.price) as avg_price, " +
            "SUM(p.stock_quantity) as total_stock " +
            "FROM categories c " +
            "LEFT JOIN products p ON p.category_id = c.id " +
            "GROUP BY c.id, c.name " +
            "ORDER BY product_count DESC",
            nativeQuery = true)
    List<Object[]> getCategoryStatistics();

    // Batch update with native SQL
    @Modifying
    @Query(value = "UPDATE products SET stock_quantity = stock_quantity - :quantity " +
            "WHERE id = :productId AND stock_quantity >= :quantity",
            nativeQuery = true)
    int decrementStock(@Param("productId") Long productId,
                       @Param("quantity") Integer quantity);
}
