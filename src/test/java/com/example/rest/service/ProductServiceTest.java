package com.example.rest.service;

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
import com.example.rest.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProductService
 * Uses Mockito to mock repository dependencies
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Unit Tests")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Category testCategory;
    private Product testProduct;
    private CreateProductRequest createRequest;

    @BeforeEach
    void setUp() {
        // Setup test data
        testCategory = Category.builder()
                .id(1L)
                .name("Electronics")
                .description("Electronic items")
                .build();

        testProduct = Product.builder()
                .id(1L)
                .sku("TEST-001")
                .name("Test Product")
                .description("Test Description")
                .price(BigDecimal.valueOf(100))
                .discountPrice(BigDecimal.valueOf(80))
                .stockQuantity(50)
                .active(true)
                .category(testCategory)
                .build();

        createRequest = CreateProductRequest.builder()
                .sku("NEW-001")
                .name("New Product")
                .description("New Description")
                .price(BigDecimal.valueOf(200))
                .stockQuantity(100)
                .categoryId(1L)
                .build();
    }

    // ============================================
    // CREATE TESTS
    // ============================================

    @Test
    @DisplayName("Should create product successfully")
    void testCreateProduct_Success() {
        // Arrange
        when(productRepository.existsBySku("NEW-001")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // Act
        ProductDTO result = productService.createProduct(createRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSku()).isEqualTo(testProduct.getSku());
        assertThat(result.getName()).isEqualTo(testProduct.getName());
        assertThat(result.getActive()).isTrue();

        // Verify interactions
        verify(productRepository).existsBySku("NEW-001");
        verify(categoryRepository).findById(1L);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw DuplicateSkuException when SKU already exists")
    void testCreateProduct_DuplicateSku() {
        // Arrange
        when(productRepository.existsBySku("NEW-001")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> productService.createProduct(createRequest))
                .isInstanceOf(DuplicateSkuException.class)
                .hasMessageContaining("NEW-001");

        // Verify
        verify(productRepository).existsBySku("NEW-001");
        verify(categoryRepository, never()).findById(any());
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw CategoryNotFoundException when category doesn't exist")
    void testCreateProduct_CategoryNotFound() {
        // Arrange
        when(productRepository.existsBySku("NEW-001")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> productService.createProduct(createRequest))
                .isInstanceOf(CategoryNotFoundException.class);

        // Verify
        verify(productRepository).existsBySku("NEW-001");
        verify(categoryRepository).findById(1L);
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when discount price exceeds regular price")
    void testCreateProduct_InvalidDiscountPrice() {
        // Arrange
        createRequest.setDiscountPrice(BigDecimal.valueOf(300));  // Higher than price
        when(productRepository.existsBySku("NEW-001")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));

        // Act & Assert
        assertThatThrownBy(() -> productService.createProduct(createRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Discount price cannot be greater");
    }

    // ============================================
    // READ TESTS
    // ============================================

    @Test
    @DisplayName("Should get product by ID successfully")
    void testGetProductById_Success() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(reviewRepository.findAverageRatingByProductId(1L)).thenReturn(4.5);
        when(reviewRepository.countByProductId(1L)).thenReturn(10L);

        // Act
        ProductDTO result = productService.getProductById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test Product");
        assertThat(result.getAverageRating()).isEqualTo(4.5);
        assertThat(result.getReviewCount()).isEqualTo(10L);

        // Verify
        verify(productRepository).findById(1L);
        verify(reviewRepository).findAverageRatingByProductId(1L);
        verify(reviewRepository).countByProductId(1L);
    }

    @Test
    @DisplayName("Should throw ProductNotFoundException when product doesn't exist")
    void testGetProductById_NotFound() {
        // Arrange
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> productService.getProductById(999L))
                .isInstanceOf(ProductNotFoundException.class);

        // Verify
        verify(productRepository).findById(999L);
    }

    @Test
    @DisplayName("Should get product by SKU successfully")
    void testGetProductBySku_Success() {
        // Arrange
        when(productRepository.findBySku("TEST-001")).thenReturn(Optional.of(testProduct));
        when(reviewRepository.findAverageRatingByProductId(1L)).thenReturn(null);  // No reviews
        when(reviewRepository.countByProductId(1L)).thenReturn(0L);

        // Act
        ProductDTO result = productService.getProductBySku("TEST-001");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSku()).isEqualTo("TEST-001");
        assertThat(result.getAverageRating()).isEqualTo(0.0);  // Defaults to 0.0
        assertThat(result.getReviewCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Should get all products with pagination")
    void testGetAllProducts() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> productPage = new PageImpl<>(List.of(testProduct));
        when(productRepository.findAll(pageable)).thenReturn(productPage);

        // Act
        Page<ProductDTO> result = productService.getAllProducts(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Test Product");

        // Verify
        verify(productRepository).findAll(pageable);
    }

    @Test
    @DisplayName("Should search products with criteria")
    void testSearchProducts() {
        // Arrange
        ProductSearchCriteria criteria = ProductSearchCriteria.builder()
                .keyword("test")
                .minPrice(BigDecimal.valueOf(50))
                .maxPrice(BigDecimal.valueOf(150))
                .activeOnly(true)
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> productPage = new PageImpl<>(List.of(testProduct));

        when(productRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(productPage);

        // Act
        Page<ProductDTO> result = productService.searchProducts(criteria, pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        // Verify
        verify(productRepository).findAll(any(Specification.class), eq(pageable));
    }

    // ============================================
    // UPDATE TESTS
    // ============================================

    @Test
    @DisplayName("Should update product successfully")
    void testUpdateProduct_Success() {
        // Arrange
        UpdateProductRequest updateRequest = UpdateProductRequest.builder()
                .name("Updated Name")
                .price(BigDecimal.valueOf(150))
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenReturn(testProduct);

        // Act
        ProductDTO result = productService.updateProduct(1L, updateRequest);

        // Assert
        assertThat(result).isNotNull();

        // Verify
        verify(productRepository).findById(1L);
        verify(productRepository).save(testProduct);
    }

    @Test
    @DisplayName("Should throw exception when updating with negative price")
    void testUpdateProduct_InvalidPrice() {
        // Arrange
        UpdateProductRequest updateRequest = UpdateProductRequest.builder()
                .price(BigDecimal.valueOf(-10))
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // Act & Assert
        assertThatThrownBy(() -> productService.updateProduct(1L, updateRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Price must be greater than 0");
    }

    // ============================================
    // ACTIVATION TESTS
    // ============================================

    @Test
    @DisplayName("Should activate product successfully")
    void testActivateProduct() {
        // Arrange
        testProduct.setActive(false);
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(testProduct)).thenReturn(testProduct);

        // Act
        productService.activateProduct(1L);

        // Assert
        assertThat(testProduct.getActive()).isTrue();

        // Verify
        verify(productRepository).findById(1L);
        verify(productRepository).save(testProduct);
    }

    @Test
    @DisplayName("Should deactivate product successfully")
    void testDeactivateProduct() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(testProduct)).thenReturn(testProduct);

        // Act
        productService.deactivateProduct(1L);

        // Assert
        assertThat(testProduct.getActive()).isFalse();

        // Verify
        verify(productRepository).findById(1L);
        verify(productRepository).save(testProduct);
    }

    // ============================================
    // DELETE TESTS
    // ============================================

    @Test
    @DisplayName("Should soft delete product successfully")
    void testDeleteProduct() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(testProduct)).thenReturn(testProduct);

        // Act
        productService.deleteProduct(1L);

        // Assert
        assertThat(testProduct.getActive()).isFalse();

        // Verify
        verify(productRepository).findById(1L);
        verify(productRepository).save(testProduct);
    }

    // ============================================
    // UTILITY TESTS
    // ============================================

    @Test
    @DisplayName("Should check if SKU exists")
    void testExistsBySku() {
        // Arrange
        when(productRepository.existsBySku("TEST-001")).thenReturn(true);

        // Act
        boolean exists = productService.existsBySku("TEST-001");

        // Assert
        assertThat(exists).isTrue();

        // Verify
        verify(productRepository).existsBySku("TEST-001");
    }
}
