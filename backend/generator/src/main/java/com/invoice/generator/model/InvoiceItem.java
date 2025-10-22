package com.invoice.generator.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "invoice_items")
public class InvoiceItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerUnit; // This will be the price *after* discount

    // --- ADD THIS NEW FIELD ---
    @Column(precision = 5, scale = 2, columnDefinition = "DECIMAL(5,2) DEFAULT 0.00")
    private BigDecimal discountPercentage;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal gstAmount;
    
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount; // This is pricePerUnit + gstAmount

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
}