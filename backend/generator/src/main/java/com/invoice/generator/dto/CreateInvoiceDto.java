package com.invoice.generator.dto;

import com.invoice.generator.model.Invoice;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateInvoiceDto {
    private Long customerId;
    private String newCustomerName;
    private String newCustomerPhone;
    private String newCustomerEmail;
    private Invoice.Status status;
    private List<InvoiceItemDto> items;
    private BigDecimal initialAmountPaid;
    private String paymentMethod;
    
    // --- NEW FIELD ---
    private boolean applyCredit; // Flag to indicate if customer credit should be used
}