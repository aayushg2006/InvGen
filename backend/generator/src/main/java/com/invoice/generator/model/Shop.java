package com.invoice.generator.model;

import jakarta.persistence.*;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "shops")
public class Shop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String shopName;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(unique = true)
    private String gstin;

    @Column(name = "logo_path")
    private String logoPath;

    @Column(name = "invoice_accent_color")
    private String invoiceAccentColor;

    @Column(name = "invoice_title")
    private String invoiceTitle;

    @Column(name = "invoice_footer", columnDefinition = "TEXT")
    private String invoiceFooter;

    // --- NEW FIELDS FOR RAZORPAY ROUTE ---
    @Column(name = "razorpay_fund_account_id")
    private String razorpayFundAccountId; // This will store the Fund Account ID from Razorpay

    @Column(name = "bank_account_number")
    private String bankAccountNumber;

    @Column(name = "bank_ifsc_code")
    private String bankIfscCode;

    @Column(name = "beneficiary_name")
    private String beneficiaryName;

    @Column(name = "payments_enabled")
    private Boolean paymentsEnabled = false; // A flag to know if they've set up payments

    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<User> users = new ArrayList<>();
}