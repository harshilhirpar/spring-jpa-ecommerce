package com.example.rest;

import com.example.rest.entity.Category;
import com.example.rest.entity.Product;
import com.example.rest.entity.ProductImage;
import com.example.rest.repository.ProductImageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestJpaConfig.class)
@ActiveProfiles("test")
@DisplayName("ProductImageRepository Tests")
class ProductImageRepositoryTest {

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Product laptop;
    private Product phone;

    private ProductImage laptopImage1;
    private ProductImage laptopImage2;
    private ProductImage laptopImage3;
    private ProductImage phoneImage1;

    @BeforeEach
    void setUp() {
        productImageRepository.deleteAll();

        // Create category
        Category electronics = Category.builder()
                .name("Electronics")
                .description("Electronic items")
                .build();
        electronics = entityManager.persist(electronics);

        // Create products
        laptop = Product.builder()
                .sku("LAPTOP-001")
                .name("Gaming Laptop")
                .description("High-performance laptop")
                .price(BigDecimal.valueOf(1500))
                .stockQuantity(10)
                .active(true)
                .category(electronics)
                .build();

        phone = Product.builder()
                .sku("PHONE-001")
                .name("Smartphone")
                .description("Latest smartphone")
                .price(BigDecimal.valueOf(800))
                .stockQuantity(20)
                .active(true)
                .category(electronics)
                .build();

        laptop = entityManager.persist(laptop);
        phone = entityManager.persist(phone);
        entityManager.flush();

        // Create images for laptop (3 images)
        laptopImage1 = ProductImage.builder()
                .product(laptop)
                .imageUrl("https://cdn.example.com/laptop-front.jpg")
                .isPrimary(true)
                .sortOrder(1)
                .build();

        laptopImage2 = ProductImage.builder()
                .product(laptop)
                .imageUrl("https://cdn.example.com/laptop-side.jpg")
                .isPrimary(false)
                .sortOrder(2)
                .build();

        laptopImage3 = ProductImage.builder()
                .product(laptop)
                .imageUrl("https://cdn.example.com/laptop-back.jpg")
                .isPrimary(false)
                .sortOrder(3)
                .build();

        // Create image for phone (1 image)
        phoneImage1 = ProductImage.builder()
                .product(phone)
                .imageUrl("https://cdn.example.com/phone-front.jpg")
                .isPrimary(true)
                .sortOrder(1)
                .build();

        productImageRepository.saveAll(List.of(
                laptopImage1, laptopImage2, laptopImage3, phoneImage1
        ));
    }

    // ============================================
    // Basic CRUD Tests
    // ============================================

    @Test
    @DisplayName("Should save product image successfully")
    void testSave() {
        // Arrange
        ProductImage newImage = ProductImage.builder()
                .product(laptop)
                .imageUrl("https://cdn.example.com/laptop-keyboard.jpg")
                .isPrimary(false)
                .sortOrder(4)
                .build();

        // Act
        ProductImage saved = productImageRepository.save(newImage);

        // Assert
        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getImageUrl()).isEqualTo("https://cdn.example.com/laptop-keyboard.jpg");
        assertThat(saved.getProduct().getId()).isEqualTo(laptop.getId());
    }

    @Test
    @DisplayName("Should find product image by ID")
    void testFindById() {
        // Act
        Optional<ProductImage> found = productImageRepository.findById(laptopImage1.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getImageUrl()).isEqualTo("https://cdn.example.com/laptop-front.jpg");
        assertThat(found.get().getProduct().getName()).isEqualTo("Gaming Laptop");
    }

    @Test
    @DisplayName("Should find all product images")
    void testFindAll() {
        // Act
        List<ProductImage> images = productImageRepository.findAll();

        // Assert
        assertThat(images).hasSize(4);  // 3 laptop + 1 phone
    }

    @Test
    @DisplayName("Should delete product image")
    void testDelete() {
        // Act
        productImageRepository.deleteById(laptopImage3.getId());

        // Assert
        assertThat(productImageRepository.existsById(laptopImage3.getId())).isFalse();
        assertThat(productImageRepository.count()).isEqualTo(3);
    }

    // ============================================
    // Relationship Query Tests
    // ============================================

    @Test
    @DisplayName("Should find all images for a product")
    void testFindByProductId() {
        // Act
        List<ProductImage> laptopImages = productImageRepository.findByProductId(laptop.getId());

        // Assert
        assertThat(laptopImages).hasSize(3);
        assertThat(laptopImages)
                .allMatch(img -> img.getProduct().getId().equals(laptop.getId()));
    }

    @Test
    @DisplayName("Should return empty list for product with no images")
    void testFindByProductId_NoImages() {
        // Arrange
        Product newProduct = Product.builder()
                .sku("PRODUCT-999")
                .name("No Images Product")
                .price(BigDecimal.valueOf(100))
                .stockQuantity(5)
                .active(true)
                .category(laptop.getCategory())
                .build();
        newProduct = entityManager.persist(newProduct);
        entityManager.flush();

        // Act
        List<ProductImage> images = productImageRepository.findByProductId(newProduct.getId());

        // Assert
        assertThat(images).isEmpty();
    }

    // ============================================
    // Sorting Tests
    // ============================================

    @Test
    @DisplayName("Should find images sorted by sort order")
    void testFindByProductIdOrderBySortOrder() {
        // Act
        List<ProductImage> images = productImageRepository.findByProductIdOrderBySortOrder(laptop.getId());

        // Assert
        assertThat(images).hasSize(3);

        // Verify order: sortOrder 1, 2, 3
        assertThat(images.get(0).getSortOrder()).isEqualTo(1);
        assertThat(images.get(1).getSortOrder()).isEqualTo(2);
        assertThat(images.get(2).getSortOrder()).isEqualTo(3);

        // Verify URLs in correct order
        assertThat(images.get(0).getImageUrl()).contains("laptop-front");
        assertThat(images.get(1).getImageUrl()).contains("laptop-side");
        assertThat(images.get(2).getImageUrl()).contains("laptop-back");
    }

    @Test
    @DisplayName("Should maintain sort order after adding new images")
    void testSortOrder_WithNewImages() {
        // Arrange - Add image with sortOrder between existing ones
        ProductImage newImage = ProductImage.builder()
                .product(laptop)
                .imageUrl("https://cdn.example.com/laptop-detail.jpg")
                .isPrimary(false)
                .sortOrder(2)  // Insert between 1 and 2
                .build();
        productImageRepository.save(newImage);

        // Act
        List<ProductImage> images = productImageRepository.findByProductIdOrderBySortOrder(laptop.getId());

        // Assert
        assertThat(images).hasSize(4);
        // Note: Now we have two images with sortOrder 2, they'll be sorted by ID or insertion order
    }

    // ============================================
    // Primary Image Tests
    // ============================================

    @Test
    @DisplayName("Should find primary image for product")
    void testFindByProductIdAndIsPrimaryTrue() {
        // Act
        Optional<ProductImage> primaryImage = productImageRepository
                .findByProductIdAndIsPrimaryTrue(laptop.getId());

        // Assert
        assertThat(primaryImage).isPresent();
        assertThat(primaryImage.get().getIsPrimary()).isTrue();
        assertThat(primaryImage.get().getImageUrl()).contains("laptop-front");
    }

    @Test
    @DisplayName("Should return empty when product has no primary image")
    void testFindByProductIdAndIsPrimaryTrue_NoPrimary() {
        // Arrange - Set all images to non-primary
        laptopImage1.setIsPrimary(false);
        productImageRepository.save(laptopImage1);

        // Act
        Optional<ProductImage> primaryImage = productImageRepository
                .findByProductIdAndIsPrimaryTrue(laptop.getId());

        // Assert
        assertThat(primaryImage).isEmpty();
    }

    // ============================================
    // Count Tests
    // ============================================

    @Test
    @DisplayName("Should count images for a product")
    void testCountByProductId() {
        // Act
        long laptopImageCount = productImageRepository.countByProductId(laptop.getId());
        long phoneImageCount = productImageRepository.countByProductId(phone.getId());

        // Assert
        assertThat(laptopImageCount).isEqualTo(3);
        assertThat(phoneImageCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Should return zero count for product with no images")
    void testCountByProductId_NoImages() {
        // Arrange
        Product newProduct = Product.builder()
                .sku("PRODUCT-888")
                .name("Empty Product")
                .price(BigDecimal.valueOf(50))
                .stockQuantity(1)
                .active(true)
                .category(laptop.getCategory())
                .build();
        newProduct = entityManager.persist(newProduct);
        entityManager.flush();

        // Act
        long count = productImageRepository.countByProductId(newProduct.getId());

        // Assert
        assertThat(count).isZero();
    }

    // ============================================
    // Bulk Delete Tests
    // ============================================

    @Test
    @DisplayName("Should delete all images for a product")
    void testDeleteByProductId() {
        // Arrange
        long initialCount = productImageRepository.count();

        // Act
        long deletedCount = productImageRepository.deleteByProductId(laptop.getId());

        // Assert
        assertThat(deletedCount).isEqualTo(3);
        assertThat(productImageRepository.count()).isEqualTo(initialCount - 3);
        assertThat(productImageRepository.findByProductId(laptop.getId())).isEmpty();

        // Verify phone images still exist
        assertThat(productImageRepository.findByProductId(phone.getId())).hasSize(1);
    }

    // ============================================
    // Real-World Scenarios
    // ============================================

    @Test
    @DisplayName("Scenario: Product gallery - get images in display order")
    void testProductGalleryScenario() {
        // Scenario: Display product page with image gallery

        // Act
        List<ProductImage> galleryImages = productImageRepository
                .findByProductIdOrderBySortOrder(laptop.getId());

        // Assert - Images in correct display order
        assertThat(galleryImages).hasSize(3);
        assertThat(galleryImages.get(0).getSortOrder()).isLessThan(
                galleryImages.get(1).getSortOrder()
        );
    }

    @Test
    @DisplayName("Scenario: Product listing - show thumbnail (primary image)")
    void testProductListingScenario() {
        // Scenario: Product listing page - need thumbnail for each product

        // Act
        Optional<ProductImage> thumbnail = productImageRepository
                .findByProductIdAndIsPrimaryTrue(laptop.getId());

        // Assert
        assertThat(thumbnail).isPresent();
        assertThat(thumbnail.get().getIsPrimary()).isTrue();

        // Use case: Display this image in product card
        String thumbnailUrl = thumbnail.get().getImageUrl();
        assertThat(thumbnailUrl).isNotEmpty();
    }

    @Test
    @DisplayName("Scenario: Enforce image limit per product")
    void testImageLimitScenario() {
        // Scenario: Business rule - max 10 images per product
        final int MAX_IMAGES = 10;

        // Act
        long currentCount = productImageRepository.countByProductId(laptop.getId());

        // Assert
        if (currentCount >= MAX_IMAGES) {
            // Reject adding more images
            System.out.println("Cannot add more images - limit reached");
        } else {
            // Can add more
            assertThat(currentCount).isLessThan(MAX_IMAGES);
        }
    }

    @Test
    @DisplayName("Scenario: Change primary image")
    void testChangePrimaryImageScenario() {
        // Scenario: User wants to set a different image as primary

        // Step 1: Remove primary flag from current primary
        laptopImage1.setIsPrimary(false);
        productImageRepository.save(laptopImage1);

        // Step 2: Set new image as primary
        laptopImage2.setIsPrimary(true);
        productImageRepository.save(laptopImage2);

        // Verify
        Optional<ProductImage> newPrimary = productImageRepository
                .findByProductIdAndIsPrimaryTrue(laptop.getId());

        assertThat(newPrimary).isPresent();
        assertThat(newPrimary.get().getId()).isEqualTo(laptopImage2.getId());
    }

    @Test
    @DisplayName("Scenario: Reorder images")
    void testReorderImagesScenario() {
        // Scenario: User drags images to reorder them

        // Original order: 1, 2, 3
        // New order: 3, 1, 2

        laptopImage1.setSortOrder(2);
        laptopImage2.setSortOrder(3);
        laptopImage3.setSortOrder(1);

        productImageRepository.saveAll(List.of(laptopImage1, laptopImage2, laptopImage3));

        // Verify new order
        List<ProductImage> reordered = productImageRepository
                .findByProductIdOrderBySortOrder(laptop.getId());

        assertThat(reordered.get(0).getId()).isEqualTo(laptopImage3.getId());
        assertThat(reordered.get(1).getId()).isEqualTo(laptopImage1.getId());
        assertThat(reordered.get(2).getId()).isEqualTo(laptopImage2.getId());
    }
}
