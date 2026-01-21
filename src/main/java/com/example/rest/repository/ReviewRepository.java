package com.example.rest.repository;

import com.example.rest.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    // Find all reviews for a product
    List<Review> findByProductId(Long productId);
    // Use case: Show all reviews on product page

    // Find reviews with pagination (for products with 1000s of reviews)
    Page<Review> findByProductId(Long productId, Pageable pageable);
    // Use case: Show 10 reviews per page

    // Find reviews by a user
    List<Review> findByUserId(Long userId);
    // Use case: Show "My Reviews" page

    // Find verified purchase reviews only
    List<Review> findByProductIdAndVerifiedPurchaseTrue(Long productId);
    // Use case: Filter to show only verified reviews

    // Find reviews by rating
    List<Review> findByProductIdAndRating(Long productId, Integer rating);
    // Use case: Filter "5-star reviews only"

    // Find reviews with rating >= threshold
    List<Review> findByProductIdAndRatingGreaterThanEqual(Long productId, Integer rating);
    // Use case: Show all reviews rated 4 or 5 stars

    // Check if user already reviewed a product
    boolean existsByProductIdAndUserId(Long productId, Long userId);
    // Use case: Prevent duplicate reviews from same user

    Optional<Review> findByProductIdAndUserId(Long productId, Long userId);
    // Use case: Load user's existing review for editing

    // Custom query: Average rating for a product
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId")
    Double findAverageRatingByProductId(@Param("productId") Long productId);
    // Use case: Display "4.5 stars" on product

    // Custom query: Rating distribution
    @Query("SELECT r.rating, COUNT(r) FROM Review r " +
            "WHERE r.product.id = :productId " +
            "GROUP BY r.rating " +
            "ORDER BY r.rating DESC")
    List<Object[]> findRatingDistribution(@Param("productId") Long productId);
    // Use case: Show bar chart "5★: 45 reviews, 4★: 23 reviews..."

    // Count reviews per product
    long countByProductId(Long productId);
    // Use case: Display "42 reviews"

    // Delete all reviews for a product
    long deleteByProductId(Long productId);
    // Use case: When deleting a product
}
