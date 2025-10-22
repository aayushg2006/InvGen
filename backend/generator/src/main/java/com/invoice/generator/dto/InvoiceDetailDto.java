package com.invoice.generator.dto;

import com.invoice.generator.model.Invoice;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class InvoiceDetailDto {
    private String invoiceNumber;
    private LocalDateTime issueDate;
    private Invoice.Status status;
    private BigDecimal subtotal;
    private BigDecimal totalGst;
    private BigDecimal grandTotal;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String shopName;
    private String shopAddress;
    private String shopGstin;
    private String shopLogoPath;
    private List<InvoiceItemDetailDto> items;
    private BigDecimal amountPaid;
    private BigDecimal balanceDue;

    // --- ADD THIS LIST ---
    private List<PaymentDetailDto> payments;
}