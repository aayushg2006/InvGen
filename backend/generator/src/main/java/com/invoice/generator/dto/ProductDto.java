package com.invoice.generator.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductDto {
    private Long id;
    private String name;
    private BigDecimal sellingPrice;
    private BigDecimal costPrice;
    private Integer quantityInStock;
    private Integer lowStockThreshold;
    private Long categoryId;
    private String categoryName;
    private BigDecimal gstPercentage;
}