package com.example.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductSearchCriteria {
    private String keyword;           // Search in name and description
    private String name;              // Search by name only
    private Long categoryId;          // Filter by category
    private BigDecimal minPrice;      // Minimum price
    private BigDecimal maxPrice;      // Maximum price
    private Integer minStock;         // Minimum stock quantity
    private Boolean activeOnly;       // Show only active products
}
