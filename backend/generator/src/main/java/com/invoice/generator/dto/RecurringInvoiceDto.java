package com.invoice.generator.dto;

import com.invoice.generator.model.RecurringInvoice;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class RecurringInvoiceDto {
    private Long id;
    private Long customerId;
    private String customerName;
    private RecurringInvoice.Frequency frequency;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate nextIssueDate;
    private boolean autoSendEmail; // --- ADD THIS FIELD ---
    private List<InvoiceItemDto> items;
}