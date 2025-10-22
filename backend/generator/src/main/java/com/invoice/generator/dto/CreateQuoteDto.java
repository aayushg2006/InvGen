package com.invoice.generator.dto;

import lombok.Data;
import java.util.List;

@Data
public class CreateQuoteDto {
    private Long customerId;
    private String newCustomerName;
    private String newCustomerPhone;
    private String newCustomerEmail;
    private List<InvoiceItemDto> items; // We can reuse the InvoiceItemDto for the items
}