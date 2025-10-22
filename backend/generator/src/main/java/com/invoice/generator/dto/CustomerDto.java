package com.invoice.generator.dto;

import lombok.Data;
import java.math.BigDecimal; // <-- ADD THIS IMPORT

@Data
public class CustomerDto {
    private Long id;
    private String name;
    private String phoneNumber;
    private String email;
    private BigDecimal creditBalance; // <-- ADD THIS LINE
}