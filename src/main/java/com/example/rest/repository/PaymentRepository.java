package com.example.rest.repository;

import com.example.rest.entity.Payment;
import com.example.rest.entity.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    // Find payment by order ID
    Optional<Payment> findByOrderId(Long orderId);
    // Use case: Get payment details for an order

    // Find payment by transaction ID
    Optional<Payment> findByTransactionId(String transactionId);
    // Use case: Lookup payment from webhook callback

    // Find all payments with a status
    List<Payment> findByStatus(PaymentStatus status);
    // Use case: Find all "PENDING" payments to retry

    // Find payments in date range
    List<Payment> findByPaymentDateBetween(LocalDateTime start, LocalDateTime end);
    // Use case: Daily payment report

    // Find failed payments for retry
    List<Payment> findByStatusAndPaymentDateBefore(
            PaymentStatus status,
            LocalDateTime cutoffDate
    );
    // Use case: Find payments that failed > 1 hour ago to retry

    // Count payments by status
    long countByStatus(PaymentStatus status);
    // Use case: Dashboard - "5 failed payments today"

    // Custom query: Total revenue
    @Query("SELECT SUM(p.amount) FROM Payment p " +
            "WHERE p.status = 'COMPLETED' " +
            "AND p.paymentDate BETWEEN :start AND :end")
    BigDecimal calculateRevenue(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
    // Use case: Monthly revenue report

    // Custom query: Revenue by payment method
    @Query("SELECT p.paymentMethod, SUM(p.amount) " +
            "FROM Payment p " +
            "WHERE p.status = 'COMPLETED' " +
            "GROUP BY p.paymentMethod")
    List<Object[]> getRevenueByPaymentMethod();
    // Use case: See how much came from credit cards vs PayPal
}
