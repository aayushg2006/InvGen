package com.invoice.generator.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "quote_items")
public class QuoteItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePerUnit; // Price after discount

    @Column(precision = 5, scale = 2, columnDefinition = "DECIMAL(5,2) DEFAULT 0.00")
    private BigDecimal discountPercentage;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal gstAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount; // pricePerUnit * quantity + gstAmount

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quote_id", nullable = false)
    private Quote quote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
}