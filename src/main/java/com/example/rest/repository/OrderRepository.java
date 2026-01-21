package com.example.rest.repository;

import com.example.rest.entity.Order;
import com.example.rest.entity.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByUserId(Long userId);

    List<Order> findByUserIdAndStatus(Long userId, OrderStatus status);

    // Date range queries
    List<Order> findByOrderDateBetween(LocalDateTime start, LocalDateTime end);

    // Find recent orders
    List<Order> findTop10ByUserIdOrderByOrderDateDesc(Long userId);

    // Count user orders
    long countByUserIdAndStatus(Long userId, OrderStatus status);

    // Paginated user orders
    Page<Order> findByUserId(Long userId, Pageable pageable);

    // Fetch order with all details
    @Query("SELECT o FROM Order o " +
            "LEFT JOIN FETCH o.orderItems oi " +
            "LEFT JOIN FETCH oi.product " +
            "LEFT JOIN FETCH o.payment " +
            "WHERE o.id = :id")
    Optional<Order> findByIdWithDetails(@Param("id") Long id);

    // User orders with total spent
    @Query("SELECT o FROM Order o " +
            "WHERE o.user.id = :userId " +
            "AND o.status IN :statuses " +
            "ORDER BY o.orderDate DESC")
    List<Order> findUserOrdersByStatuses(@Param("userId") Long userId,
                                         @Param("statuses") List<OrderStatus> statuses);

    // Sales report by date range
    @Query("SELECT DATE(o.orderDate) as date, " +
            "COUNT(o) as orderCount, " +
            "SUM(o.totalAmount) as totalSales " +
            "FROM Order o " +
            "WHERE o.orderDate BETWEEN :startDate AND :endDate " +
            "AND o.status != 'CANCELLED' " +
            "GROUP BY DATE(o.orderDate) " +
            "ORDER BY date DESC")
    List<Object[]> getSalesReport(@Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);

    // Calculate user lifetime value
    @Query("SELECT SUM(o.totalAmount) FROM Order o " +
            "WHERE o.user.id = :userId " +
            "AND o.status IN ('DELIVERED', 'SHIPPED')")
    Optional<BigDecimal> calculateUserLifetimeValue(@Param("userId") Long userId);

    // Pending orders older than X days
    @Query("SELECT o FROM Order o " +
            "WHERE o.status = 'PENDING' " +
            "AND o.orderDate < :cutoffDate")
    List<Order> findPendingOrdersOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
}
