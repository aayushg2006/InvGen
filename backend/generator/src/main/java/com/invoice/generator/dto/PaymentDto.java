package com.invoice.generator.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentDto {
    private BigDecimal amount;
    private String paymentMethod;
    
    // --- NEW FIELDS ---
    private boolean sendReceipt; // To know whether to email a receipt
    private String overpaymentChoice; // Will be "credit" or "refund"

    public enum OverpaymentChoice {
        CREDIT,
        REFUND
    }
}