package com.invoice.generator.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "recurring_invoice_items")
public class RecurringInvoiceItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    private BigDecimal discountPercentage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recurring_invoice_id", nullable = false)
    private RecurringInvoice recurringInvoice;
}