package com.invoice.generator.dto;

import lombok.Data;
import java.math.BigDecimal; // Make sure this is imported

@Data
public class InvoiceItemDto {
    private Long productId;
    private int quantity;

    // --- ADD THIS NEW FIELD ---
    private BigDecimal discountPercentage;
}