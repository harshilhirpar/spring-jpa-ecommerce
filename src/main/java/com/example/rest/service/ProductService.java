package com.example.rest.service;

import com.example.rest.dto.CreateProductRequest;
import com.example.rest.dto.ProductDTO;
import com.example.rest.dto.ProductSearchCriteria;
import com.example.rest.dto.UpdateProductRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    /**
     * Create a new product
     * @param request product creation data
     * @return created product DTO
     * @throws com.example.rest.exception.DuplicateSkuException if SKU already exists
     * @throws com.example.rest.exception.CategoryNotFoundException if category not found
     */
    ProductDTO createProduct(CreateProductRequest request);

    /**
     * Update an existing product (partial update)
     * @param id product ID
     * @param request update data
     * @return updated product DTO
     * @throws com.example.rest.exception.ProductNotFoundException if product not found
     */
    ProductDTO updateProduct(Long id, UpdateProductRequest request);

    /**
     * Get product by ID
     * @param id product ID
     * @return product DTO with review statistics
     * @throws com.example.rest.exception.ProductNotFoundException if product not found
     */
    ProductDTO getProductById(Long id);

    /**
     * Get product by SKU
     * @param sku product SKU
     * @return product DTO
     * @throws ProductNotFoundException if product not found
     */
    ProductDTO getProductBySku(String sku);

    /**
     * Get all products with pagination
     * @param pageable pagination parameters
     * @return page of product DTOs
     */
    Page<ProductDTO> getAllProducts(Pageable pageable);

    /**
     * Get only active products
     * @param pageable pagination parameters
     * @return page of active product DTOs
     */
    Page<ProductDTO> getActiveProducts(Pageable pageable);

    /**
     * Search products with criteria
     * @param criteria search criteria
     * @param pageable pagination parameters
     * @return page of matching product DTOs
     */
    Page<ProductDTO> searchProducts(ProductSearchCriteria criteria, Pageable pageable);

    /**
     * Get products by category
     * @param categoryId category ID
     * @param pageable pagination parameters
     * @return page of product DTOs
     */
    Page<ProductDTO> getProductsByCategory(Long categoryId, Pageable pageable);

    /**
     * Soft delete a product (set active = false)
     * @param id product ID
     * @throws ProductNotFoundException if product not found
     */
    void deleteProduct(Long id);

    /**
     * Activate a product
     * @param id product ID
     * @throws ProductNotFoundException if product not found
     */
    void activateProduct(Long id);

    /**
     * Deactivate a product
     * @param id product ID
     * @throws ProductNotFoundException if product not found
     */
    void deactivateProduct(Long id);

    /**
     * Check if product exists by SKU
     * @param sku product SKU
     * @return true if exists
     */
    boolean existsBySku(String sku);
}
