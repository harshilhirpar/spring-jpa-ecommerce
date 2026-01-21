package com.example.rest.service.impl;
import com.example.rest.dto.CreateProductRequest;
import com.example.rest.dto.ProductDTO;
import com.example.rest.dto.ProductSearchCriteria;
import com.example.rest.dto.UpdateProductRequest;
import com.example.rest.entity.Category;
import com.example.rest.entity.Product;
import com.example.rest.exception.CategoryNotFoundException;
import com.example.rest.exception.DuplicateSkuException;
import com.example.rest.exception.ProductNotFoundException;
import com.example.rest.repository.CategoryRepository;
import com.example.rest.repository.ProductRepository;
import com.example.rest.repository.ReviewRepository;
import com.example.rest.service.ProductService;
import com.example.rest.specification.ProductSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Implementation of ProductService
 * Handles all business logic for products
 */
@Service
@Transactional(readOnly = true)  // All methods read-only by default
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ReviewRepository reviewRepository;

    // ============================================
    // CREATE
    // ============================================

    @Override
    @Transactional  // Override to allow writes
    public ProductDTO createProduct(CreateProductRequest request) {
        log.info("Creating product with SKU: {}", request.getSku());

        // Business Rule 1: SKU must be unique
        if (productRepository.existsBySku(request.getSku())) {
            log.warn("Attempted to create product with duplicate SKU: {}", request.getSku());
            throw new DuplicateSkuException(request.getSku());
        }

        // Business Rule 2: Category must exist
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> {
                    log.error("Category not found with ID: {}", request.getCategoryId());
                    return new CategoryNotFoundException(request.getCategoryId());
                });

        // Business Rule 3: Validate discount price
        if (request.getDiscountPrice() != null &&
                request.getDiscountPrice().compareTo(request.getPrice()) > 0) {
            log.warn("Discount price cannot be greater than regular price");
            throw new IllegalArgumentException("Discount price cannot be greater than regular price");
        }

        // Create product entity
        Product product = Product.builder()
                .sku(request.getSku())
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .discountPrice(request.getDiscountPrice())
                .stockQuantity(request.getStockQuantity())
                .category(category)
                .active(true)  // New products are active by default
                .build();

        // Save to database
        Product savedProduct = productRepository.save(product);

        log.info("Product created successfully with ID: {} and SKU: {}",
                savedProduct.getId(), savedProduct.getSku());

        // Convert to DTO and return
        return convertToDTO(savedProduct);
    }

    // ============================================
    // READ
    // ============================================

    @Override
    public ProductDTO getProductById(Long id) {
        log.info("Fetching product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Product not found with ID: {}", id);
                    return new ProductNotFoundException(id);
                });

        return convertToDTOWithReviewStats(product);
    }

    @Override
    public ProductDTO getProductBySku(String sku) {
        log.info("Fetching product with SKU: {}", sku);

        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> {
                    log.error("Product not found with SKU: {}", sku);
                    return new ProductNotFoundException("Product not found with SKU: " + sku);
                });

        return convertToDTOWithReviewStats(product);
    }

    @Override
    public Page<ProductDTO> getAllProducts(Pageable pageable) {
        log.info("Fetching all products - Page: {}, Size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        return productRepository.findAll(pageable)
                .map(this::convertToDTO);
    }

    @Override
    public Page<ProductDTO> getActiveProducts(Pageable pageable) {
        log.info("Fetching active products - Page: {}, Size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        return productRepository.findByActiveTrue(pageable)
                .map(this::convertToDTO);
    }

    @Override
    public Page<ProductDTO> searchProducts(ProductSearchCriteria criteria, Pageable pageable) {
        log.info("Searching products with criteria: {}", criteria);

        // Build dynamic query using Specifications
        Specification<Product> spec = Specification.where((Specification<Product>) null);

        // Add active filter if specified
        if (criteria.getActiveOnly() != null && criteria.getActiveOnly()) {
            spec = spec.and(ProductSpecification.isActive());
        }

        // Add keyword search
        if (criteria.getKeyword() != null && !criteria.getKeyword().trim().isEmpty()) {
            spec = spec.and(ProductSpecification.searchByKeyword(criteria.getKeyword()));
        }

        // Add name filter
        if (criteria.getName() != null && !criteria.getName().trim().isEmpty()) {
            spec = spec.and(ProductSpecification.hasName(criteria.getName()));
        }

        // Add category filter
        if (criteria.getCategoryId() != null) {
            spec = spec.and(ProductSpecification.hasCategory(criteria.getCategoryId()));
        }

        // Add price range filter
        if (criteria.getMinPrice() != null || criteria.getMaxPrice() != null) {
            spec = spec.and(ProductSpecification.hasPriceBetween(
                    criteria.getMinPrice(), criteria.getMaxPrice()));
        }

        // Add stock filter
        if (criteria.getMinStock() != null) {
            spec = spec.and(ProductSpecification.hasStockGreaterThan(criteria.getMinStock()));
        }

        return productRepository.findAll(spec, pageable)
                .map(this::convertToDTO);
    }

    @Override
    public Page<ProductDTO> getProductsByCategory(Long categoryId, Pageable pageable) {
        log.info("Fetching products for category ID: {}", categoryId);

        // Verify category exists
        if (!categoryRepository.existsById(categoryId)) {
            throw new CategoryNotFoundException(categoryId);
        }

        return productRepository.findByCategoryIdAndActiveTrue(categoryId, pageable)
                .map(this::convertToDTO);
    }

    // ============================================
    // UPDATE
    // ============================================

    @Override
    @Transactional
    public ProductDTO updateProduct(Long id, UpdateProductRequest request) {
        log.info("Updating product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Product not found with ID: {}", id);
                    return new ProductNotFoundException(id);
                });

        // Update only provided fields (partial update)
        boolean updated = false;

        if (request.getName() != null && !request.getName().equals(product.getName())) {
            product.setName(request.getName());
            updated = true;
        }

        if (request.getDescription() != null && !request.getDescription().equals(product.getDescription())) {
            product.setDescription(request.getDescription());
            updated = true;
        }

        if (request.getPrice() != null && !request.getPrice().equals(product.getPrice())) {
            // Business Rule: Price must be positive
            if (request.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Price must be greater than 0");
            }
            product.setPrice(request.getPrice());
            updated = true;
        }

        if (request.getDiscountPrice() != null) {
            // Business Rule: Discount price cannot exceed regular price
            BigDecimal priceToCompare = request.getPrice() != null ? request.getPrice() : product.getPrice();
            if (request.getDiscountPrice().compareTo(priceToCompare) > 0) {
                throw new IllegalArgumentException("Discount price cannot be greater than regular price");
            }
            product.setDiscountPrice(request.getDiscountPrice());
            updated = true;
        }

        if (request.getStockQuantity() != null && !request.getStockQuantity().equals(product.getStockQuantity())) {
            // Business Rule: Stock cannot be negative
            if (request.getStockQuantity() < 0) {
                throw new IllegalArgumentException("Stock quantity cannot be negative");
            }
            product.setStockQuantity(request.getStockQuantity());
            updated = true;
        }

        if (request.getCategoryId() != null && !request.getCategoryId().equals(product.getCategory().getId())) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new CategoryNotFoundException(request.getCategoryId()));
            product.setCategory(category);
            updated = true;
        }

        if (!updated) {
            log.info("No changes detected for product ID: {}", id);
            return convertToDTO(product);
        }

        Product updatedProduct = productRepository.save(product);

        log.info("Product updated successfully with ID: {}", id);

        return convertToDTO(updatedProduct);
    }

    @Override
    @Transactional
    public void activateProduct(Long id) {
        log.info("Activating product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        if (product.getActive()) {
            log.info("Product ID: {} is already active", id);
            return;
        }

        product.setActive(true);
        productRepository.save(product);

        log.info("Product activated successfully with ID: {}", id);
    }

    @Override
    @Transactional
    public void deactivateProduct(Long id) {
        log.info("Deactivating product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        if (!product.getActive()) {
            log.info("Product ID: {} is already inactive", id);
            return;
        }

        product.setActive(false);
        productRepository.save(product);

        log.info("Product deactivated successfully with ID: {}", id);
    }

    // ============================================
    // DELETE (Soft Delete)
    // ============================================

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        log.info("Soft deleting product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        // Soft delete - just deactivate
        product.setActive(false);
        productRepository.save(product);

        log.info("Product soft deleted (deactivated) successfully with ID: {}", id);
    }

    // ============================================
    // UTILITY
    // ============================================

    @Override
    public boolean existsBySku(String sku) {
        return productRepository.existsBySku(sku);
    }

    // ============================================
    // PRIVATE HELPER METHODS
    // ============================================

    /**
     * Convert Product entity to ProductDTO (basic)
     */
    private ProductDTO convertToDTO(Product product) {
        return ProductDTO.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .discountPrice(product.getDiscountPrice())
                .effectivePrice(product.getEffectivePrice())
                .stockQuantity(product.getStockQuantity())
                .active(product.getActive())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .createdBy(product.getCreatedBy())
                .lastModifiedBy(product.getLastModifiedBy())
                .build();
    }

    /**
     * Convert Product entity to ProductDTO with review statistics
     */
    private ProductDTO convertToDTOWithReviewStats(Product product) {
        ProductDTO dto = convertToDTO(product);

        // Add review statistics
        Double avgRating = reviewRepository.findAverageRatingByProductId(product.getId());
        Long reviewCount = reviewRepository.countByProductId(product.getId());

        dto.setAverageRating(avgRating != null ? avgRating : 0.0);
        dto.setReviewCount(reviewCount);

        return dto;
    }
}
