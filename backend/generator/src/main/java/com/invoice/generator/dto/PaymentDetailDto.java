package com.invoice.generator.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentDetailDto {
    private Long id; // <-- ADD THIS LINE
    private BigDecimal amount;
    private LocalDateTime paymentDate;
    private String paymentMethod;
}