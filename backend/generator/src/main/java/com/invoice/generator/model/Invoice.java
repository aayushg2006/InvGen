package com.invoice.generator.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "invoices")
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalGst;

    @Column(nullable = false)
    private LocalDateTime issueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    @Column(precision = 12, scale = 2)
    private BigDecimal amountPaid;

    @Column(precision = 12, scale = 2)
    private BigDecimal balanceDue;

    public enum Status {
        PENDING, PAID, PARTIALLY_PAID, CANCELLED
    }

    @OneToMany(mappedBy = "invoice", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    private List<InvoiceItem> invoiceItems = new ArrayList<>();

    // --- THIS LINE IS THE FIX ---
    // Changed FetchType to EAGER to ensure payments are always loaded with the invoice.
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Payment> payments = new ArrayList<>();
}