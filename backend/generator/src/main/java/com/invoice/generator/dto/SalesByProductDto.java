package com.invoice.generator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class SalesByProductDto {
    private String productName;
    private Long totalQuantitySold;
    private BigDecimal totalRevenue;
}