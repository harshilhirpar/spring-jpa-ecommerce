package com.example.rest.repository;

import com.example.rest.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    // Find all items in an order
    List<OrderItem> findByOrderId(Long orderId);
    // Use case: Display order details with all items

    // Find all orders containing a specific product
    List<OrderItem> findByProductId(Long productId);
    // Use case: See who bought this product

    // Count how many times a product was ordered
    long countByProductId(Long productId);
    // Use case: "Bought by 1,234 customers"

    // Custom query: Find best-selling products
    @Query("SELECT oi.product.id, oi.product.name, SUM(oi.quantity) as totalSold " +
            "FROM OrderItem oi " +
            "JOIN oi.order o " +
            "WHERE o.status = 'DELIVERED' " +
            "GROUP BY oi.product.id, oi.product.name " +
            "ORDER BY totalSold DESC")
    List<Object[]> findBestSellingProducts();
    // Use case: Dashboard showing top 10 products

    // Custom query: Total revenue for a product
    @Query("SELECT SUM(oi.unitPrice * oi.quantity) FROM OrderItem oi " +
            "WHERE oi.product.id = :productId " +
            "AND oi.order.status = 'DELIVERED'")
    BigDecimal calculateProductRevenue(@Param("productId") Long productId);
    // Use case: Product analytics

    // Custom query: Find products purchased together (co-purchase analysis)
    @Query("SELECT oi2.product.id, oi2.product.name, COUNT(oi2) as frequency " +
            "FROM OrderItem oi1 " +
            "JOIN OrderItem oi2 ON oi1.order.id = oi2.order.id " +
            "WHERE oi1.product.id = :productId " +
            "AND oi2.product.id != :productId " +
            "GROUP BY oi2.product.id, oi2.product.name " +
            "ORDER BY frequency DESC")
    List<Object[]> findFrequentlyBoughtTogether(@Param("productId") Long productId);
    // Use case: "Customers who bought this also bought..."
}
