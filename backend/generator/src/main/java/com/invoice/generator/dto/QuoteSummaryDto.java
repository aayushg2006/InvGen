package com.invoice.generator.dto;

import com.invoice.generator.model.Quote;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class QuoteSummaryDto {
    private Long id;
    private String quoteNumber;
    private String customerName;
    private BigDecimal totalAmount;
    private Quote.Status status;
    private LocalDateTime issueDate;
}