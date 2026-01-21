package com.example.rest;

import com.example.rest.entity.Category;
import com.example.rest.entity.Product;
import com.example.rest.entity.Review;
import com.example.rest.entity.User;
import com.example.rest.entity.enums.UserStatusEnum;
import com.example.rest.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestJpaConfig.class)
@ActiveProfiles("test")
@DisplayName("ReviewRepository Tests - Aggregations & Ratings")
class ReviewRepositoryTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User user1;
    private User user2;
    private User user3;

    private Product laptop;
    private Product phone;

    private Review laptopReview1;
    private Review laptopReview2;
    private Review laptopReview3;
    private Review laptopReview4;
    private Review phoneReview1;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();

        // Create users
        user1 = User.builder()
                .username("john_doe")
                .email("john@example.com")
                .password("password123")
                .status(UserStatusEnum.ACTIVE)
                .build();

        user2 = User.builder()
                .username("jane_smith")
                .email("jane@example.com")
                .password("password123")
                .status(UserStatusEnum.ACTIVE)
                .build();

        user3 = User.builder()
                .username("bob_wilson")
                .email("bob@example.com")
                .password("password123")
                .status(UserStatusEnum.ACTIVE)
                .build();

        user1 = entityManager.persist(user1);
        user2 = entityManager.persist(user2);
        user3 = entityManager.persist(user3);

        // Create category
        Category electronics = Category.builder()
                .name("Electronics")
                .build();
        electronics = entityManager.persist(electronics);

        // Create products
        laptop = Product.builder()
                .sku("LAPTOP-001")
                .name("Gaming Laptop")
                .price(BigDecimal.valueOf(1500))
                .stockQuantity(10)
                .active(true)
                .category(electronics)
                .build();

        phone = Product.builder()
                .sku("PHONE-001")
                .name("Smartphone")
                .price(BigDecimal.valueOf(800))
                .stockQuantity(20)
                .active(true)
                .category(electronics)
                .build();

        laptop = entityManager.persist(laptop);
        phone = entityManager.persist(phone);
        entityManager.flush();

        // Create reviews for laptop (4 reviews with different ratings)
        laptopReview1 = Review.builder()
                .product(laptop)
                .user(user1)
                .rating(5)
                .comment("Excellent laptop! Highly recommended.")
                .verifiedPurchase(true)
                .build();

        laptopReview2 = Review.builder()
                .product(laptop)
                .user(user2)
                .rating(4)
                .comment("Good performance, but a bit heavy.")
                .verifiedPurchase(true)
                .build();

        laptopReview3 = Review.builder()
                .product(laptop)
                .user(user3)
                .rating(5)
                .comment("Best laptop I've ever owned!")
                .verifiedPurchase(false)
                .build();

        // Create another user for 4th review
        User user4 = User.builder()
                .username("alice_johnson")
                .email("alice@example.com")
                .password("password123")
                .status(UserStatusEnum.ACTIVE)
                .build();
        user4 = entityManager.persist(user4);

        laptopReview4 = Review.builder()
                .product(laptop)
                .user(user4)
                .rating(3)
                .comment("Average product, expected better for the price.")
                .verifiedPurchase(true)
                .build();

        // Create review for phone
        phoneReview1 = Review.builder()
                .product(phone)
                .user(user1)
                .rating(4)
                .comment("Great phone for the price.")
                .verifiedPurchase(true)
                .build();

        reviewRepository.saveAll(List.of(
                laptopReview1, laptopReview2, laptopReview3, laptopReview4, phoneReview1
        ));
    }

    // ============================================
    // Basic CRUD Tests
    // ============================================

    @Test
    @DisplayName("Should save review successfully")
    void testSave() {
        // Arrange
        Review newReview = Review.builder()
                .product(phone)
                .user(user2)
                .rating(5)
                .comment("Amazing phone!")
                .verifiedPurchase(true)
                .build();

        // Act
        Review saved = reviewRepository.save(newReview);

        // Assert
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getRating()).isEqualTo(5);
        assertThat(saved.getProduct().getId()).isEqualTo(phone.getId());
        assertThat(saved.getUser().getId()).isEqualTo(user2.getId());
    }

    @Test
    @DisplayName("Should find review by ID with relationships loaded")
    void testFindById() {
        // Act
        Optional<Review> found = reviewRepository.findById(laptopReview1.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getComment()).contains("Excellent laptop");
        assertThat(found.get().getProduct().getName()).isEqualTo("Gaming Laptop");
        assertThat(found.get().getUser().getUsername()).isEqualTo("john_doe");
    }

    // ============================================
    // Product-Based Query Tests
    // ============================================

    @Test
    @DisplayName("Should find all reviews for a product")
    void testFindByProductId() {
        // Act
        List<Review> laptopReviews = reviewRepository.findByProductId(laptop.getId());

        // Assert
        assertThat(laptopReviews).hasSize(4);
        assertThat(laptopReviews)
                .allMatch(review -> review.getProduct().getId().equals(laptop.getId()));
    }

    @Test
    @DisplayName("Should find reviews with pagination")
    void testFindByProductId_Paginated() {
        // Act - Get first 2 reviews
        Page<Review> page1 = reviewRepository.findByProductId(
                laptop.getId(),
                PageRequest.of(0, 2)
        );

        // Assert
        assertThat(page1.getContent()).hasSize(2);
        assertThat(page1.getTotalElements()).isEqualTo(4);
        assertThat(page1.getTotalPages()).isEqualTo(2);
        assertThat(page1.hasNext()).isTrue();

        // Get second page
        Page<Review> page2 = reviewRepository.findByProductId(
                laptop.getId(),
                PageRequest.of(1, 2)
        );

        assertThat(page2.getContent()).hasSize(2);
        assertThat(page2.hasNext()).isFalse();
    }

    @Test
    @DisplayName("Should find verified purchase reviews only")
    void testFindByProductIdAndVerifiedPurchaseTrue() {
        // Act
        List<Review> verifiedReviews = reviewRepository
                .findByProductIdAndVerifiedPurchaseTrue(laptop.getId());

        // Assert
        assertThat(verifiedReviews).hasSize(3);  // 3 out of 4 are verified
        assertThat(verifiedReviews)
                .allMatch(Review::getVerifiedPurchase);
    }

    // ============================================
    // Rating-Based Query Tests
    // ============================================

    @Test
    @DisplayName("Should find reviews by specific rating")
    void testFindByProductIdAndRating() {
        // Act
        List<Review> fiveStarReviews = reviewRepository
                .findByProductIdAndRating(laptop.getId(), 5);

        // Assert
        assertThat(fiveStarReviews).hasSize(2);
        assertThat(fiveStarReviews)
                .allMatch(review -> review.getRating() == 5);
    }

    @Test
    @DisplayName("Should find reviews with rating greater than or equal to threshold")
    void testFindByProductIdAndRatingGreaterThanEqual() {
        // Act - Get 4 and 5 star reviews
        List<Review> highRatedReviews = reviewRepository
                .findByProductIdAndRatingGreaterThanEqual(laptop.getId(), 4);

        // Assert
        assertThat(highRatedReviews).hasSize(3);  // Two 5-star + one 4-star
        assertThat(highRatedReviews)
                .allMatch(review -> review.getRating() >= 4);
    }

    // ============================================
    // User-Based Query Tests
    // ============================================

    @Test
    @DisplayName("Should find all reviews by a user")
    void testFindByUserId() {
        // Act
        List<Review> user1Reviews = reviewRepository.findByUserId(user1.getId());

        // Assert
        assertThat(user1Reviews).hasSize(2);  // Laptop + Phone
        assertThat(user1Reviews)
                .allMatch(review -> review.getUser().getId().equals(user1.getId()));
    }

    // ============================================
    // Duplicate Prevention Tests
    // ============================================

    @Test
    @DisplayName("Should check if user already reviewed a product")
    void testExistsByProductIdAndUserId() {
        // Act & Assert
        assertThat(reviewRepository.existsByProductIdAndUserId(laptop.getId(), user1.getId()))
                .isTrue();
        assertThat(reviewRepository.existsByProductIdAndUserId(phone.getId(), user2.getId()))
                .isFalse();
    }

    @Test
    @DisplayName("Should find existing review by user and product")
    void testFindByProductIdAndUserId() {
        // Act
        Optional<Review> existingReview = reviewRepository
                .findByProductIdAndUserId(laptop.getId(), user1.getId());

        // Assert
        assertThat(existingReview).isPresent();
        assertThat(existingReview.get().getComment()).contains("Excellent laptop");
    }

    // ============================================
    // Aggregation Query Tests
    // ============================================

    @Test
    @DisplayName("Should calculate average rating for a product")
    void testFindAverageRatingByProductId() {
        // Act
        Double avgRating = reviewRepository.findAverageRatingByProductId(laptop.getId());

        // Assert
        // Ratings: 5, 4, 5, 3 → Average = 17/4 = 4.25
        assertThat(avgRating).isNotNull();
        assertThat(avgRating).isEqualTo(4.25);
    }

    @Test
    @DisplayName("Should return null for product with no reviews")
    void testFindAverageRatingByProductId_NoReviews() {
        // Arrange
        Product newProduct = Product.builder()
                .sku("PRODUCT-999")
                .name("No Reviews Product")
                .price(BigDecimal.valueOf(100))
                .stockQuantity(5)
                .active(true)
                .category(laptop.getCategory())
                .build();
        newProduct = entityManager.persist(newProduct);
        entityManager.flush();

        // Act
        Double avgRating = reviewRepository.findAverageRatingByProductId(newProduct.getId());

        // Assert
        assertThat(avgRating).isNull();
    }

    @Test
    @DisplayName("Should get rating distribution for a product")
    void testFindRatingDistribution() {
        // Act
        List<Object[]> distribution = reviewRepository
                .findRatingDistribution(laptop.getId());

        // Assert
        assertThat(distribution).isNotEmpty();

        // Expected: 5★: 2, 4★: 1, 3★: 1 (sorted DESC by rating)
        assertThat(distribution).hasSize(3);

        // First row: 5 stars, count 2
        Object[] fiveStars = distribution.get(0);
        assertThat((Integer) fiveStars[0]).isEqualTo(5);
        assertThat((Long) fiveStars[1]).isEqualTo(2L);

        // Second row: 4 stars, count 1
        Object[] fourStars = distribution.get(1);
        assertThat((Integer) fourStars[0]).isEqualTo(4);
        assertThat((Long) fourStars[1]).isEqualTo(1L);

        // Third row: 3 stars, count 1
        Object[] threeStars = distribution.get(2);
        assertThat((Integer) threeStars[0]).isEqualTo(3);
        assertThat((Long) threeStars[1]).isEqualTo(1L);
    }

    // ============================================
    // Count Tests
    // ============================================

    @Test
    @DisplayName("Should count reviews for a product")
    void testCountByProductId() {
        // Act
        long laptopReviewCount = reviewRepository.countByProductId(laptop.getId());
        long phoneReviewCount = reviewRepository.countByProductId(phone.getId());

        // Assert
        assertThat(laptopReviewCount).isEqualTo(4);
        assertThat(phoneReviewCount).isEqualTo(1);
    }

    // ============================================
    // Delete Tests
    // ============================================

    @Test
    @DisplayName("Should delete all reviews for a product")
    void testDeleteByProductId() {
        // Arrange
        long initialCount = reviewRepository.count();

        // Act
        long deletedCount = reviewRepository.deleteByProductId(laptop.getId());

        // Assert
        assertThat(deletedCount).isEqualTo(4);
        assertThat(reviewRepository.count()).isEqualTo(initialCount - 4);
        assertThat(reviewRepository.findByProductId(laptop.getId())).isEmpty();

        // Phone reviews still exist
        assertThat(reviewRepository.findByProductId(phone.getId())).hasSize(1);
    }

    // ============================================
    // Real-World Scenarios
    // ============================================

    @Test
    @DisplayName("Scenario: Product page - display average rating and review count")
    void testProductPageScenario() {
        // Act
        Double avgRating = reviewRepository.findAverageRatingByProductId(laptop.getId());
        long reviewCount = reviewRepository.countByProductId(laptop.getId());

        // Assert
        assertThat(avgRating).isEqualTo(4.25);
        assertThat(reviewCount).isEqualTo(4);

        // Display: "★★★★☆ 4.25 (4 reviews)"
        System.out.println("Rating: " + avgRating + " stars (" + reviewCount + " reviews)");
    }

    @Test
    @DisplayName("Scenario: Rating filter - show only 5-star reviews")
    void testRatingFilterScenario() {
        // Act
        List<Review> topReviews = reviewRepository
                .findByProductIdAndRating(laptop.getId(), 5);

        // Assert
        assertThat(topReviews).hasSize(2);
        assertThat(topReviews)
                .extracting(Review::getComment)
                .contains(
                        "Excellent laptop! Highly recommended.",
                        "Best laptop I've ever owned!"
                );
    }

    @Test
    @DisplayName("Scenario: Prevent duplicate reviews")
    void testPreventDuplicateReviewScenario() {
        // Scenario: User tries to review same product twice

        // Check if user already reviewed
        boolean alreadyReviewed = reviewRepository
                .existsByProductIdAndUserId(laptop.getId(), user1.getId());

        if (alreadyReviewed) {
            // Don't allow new review, show existing one for editing
            Optional<Review> existing = reviewRepository
                    .findByProductIdAndUserId(laptop.getId(), user1.getId());

            assertThat(existing).isPresent();
            System.out.println("User already reviewed this product: " + existing.get().getComment());
        } else {
            // Allow new review
            System.out.println("User can submit a review");
        }
    }

    @Test
    @DisplayName("Scenario: Show rating distribution chart")
    void testRatingDistributionChartScenario() {
        // Act
        List<Object[]> distribution = reviewRepository
                .findRatingDistribution(laptop.getId());

        // Display as bar chart
        System.out.println("Rating Distribution:");
        for (Object[] row : distribution) {
            Integer rating = (Integer) row[0];
            Long count = (Long) row[1];
            System.out.println(rating + "★: " + "█".repeat(count.intValue()) + " (" + count + ")");
        }

        // Output:
        // 5★: ██ (2)
        // 4★: █ (1)
        // 3★: █ (1)
    }

    @Test
    @DisplayName("Scenario: Verified purchase badge")
    void testVerifiedPurchaseBadgeScenario() {
        // Act
        List<Review> allReviews = reviewRepository.findByProductId(laptop.getId());

        // Display with verified badge
        for (Review review : allReviews) {
            String badge = review.getVerifiedPurchase() ? "[✓ Verified Purchase]" : "";
            System.out.println(review.getUser().getUsername() + " " + badge + ": " +
                    review.getRating() + "★ - " + review.getComment());
        }
    }

    @Test
    @DisplayName("Scenario: Update existing review")
    void testUpdateReviewScenario() {
        // Scenario: User wants to update their review

        // Find existing review
        Review existingReview = reviewRepository
                .findByProductIdAndUserId(laptop.getId(), user1.getId())
                .orElseThrow();

        // Update rating and comment
        existingReview.setRating(4);
        existingReview.setComment("Updated: Still great, but battery life could be better.");

        // Save
        Review updated = reviewRepository.save(existingReview);

        // Verify
        assertThat(updated.getRating()).isEqualTo(4);
        assertThat(updated.getComment()).contains("Updated:");

        // Recalculate average: was 4.25, now 4.0 (changed 5→4)
        Double newAvg = reviewRepository.findAverageRatingByProductId(laptop.getId());
        assertThat(newAvg).isEqualTo(4.0);
    }
}
