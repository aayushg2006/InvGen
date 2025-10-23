package com.invoice.generator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class RevenueByCustomerDto {
    private String customerName;
    private BigDecimal totalRevenue;
}