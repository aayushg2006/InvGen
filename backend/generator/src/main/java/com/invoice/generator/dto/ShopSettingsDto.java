package com.invoice.generator.dto;

import lombok.Data;

@Data
public class ShopSettingsDto {
    private String shopName;
    private String gstin;
    private String address;
    private String logoPath;
    private String invoiceAccentColor;
    private String invoiceTitle;
    private String invoiceFooter;

    // --- NEW FIELDS FOR RAZORPAY ROUTE ---
    private String bankAccountNumber;
    private String bankIfscCode;
    private String beneficiaryName;
    private Boolean paymentsEnabled;
}