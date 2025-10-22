package com.invoice.generator.dto;

import lombok.Data;

@Data
public class ShopSettingsDto {
    private String shopName;
    private String gstin;
    private String address;
    private String logoPath;

    // --- ADD THESE NEW FIELDS ---
    private String invoiceAccentColor;
    private String invoiceTitle;
    private String invoiceFooter;
}