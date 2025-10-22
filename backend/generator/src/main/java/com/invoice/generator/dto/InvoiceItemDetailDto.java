package com.invoice.generator.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class InvoiceItemDetailDto {
    private String productName;
    private int quantity;
    private BigDecimal pricePerUnit;
    private BigDecimal gstAmount;
    private BigDecimal totalAmount;
    private BigDecimal discountPercentage;
    private BigDecimal originalPrice;
}