package com.example.rest.repository;

import com.example.rest.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    // Find all images for a product
    List<ProductImage> findByProductId(Long productId);
    // Use case: Load product gallery

    // Find images sorted by order
    List<ProductImage> findByProductIdOrderBySortOrder(Long productId);
    // Use case: Display images in specific order

    // Find primary image
    Optional<ProductImage> findByProductIdAndIsPrimaryTrue(Long productId);
    // Use case: Get thumbnail for product listing

    // Count images for a product
    long countByProductId(Long productId);
    // Use case: Limit products to max 10 images

    // Delete all images for a product
    long deleteByProductId(Long productId);
    // Use case: When deleting a product
}
