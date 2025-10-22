package com.invoice.generator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class PaymentSummaryDto {
    private String paymentMethod;
    private BigDecimal totalAmount;
}