package com.invoice.generator.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "quotes")
public class Quote {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String quoteNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount; // Amount before GST

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalGst;

    @Column(nullable = false)
    private LocalDateTime issueDate;

    @Enumerated(EnumType.STRING)
    // --- THIS IS THE FIX ---
    @Column(nullable = false, length = 20)
    private Status status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", nullable = false)
    private Shop shop;

    // A quote can be converted into an invoice
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    public enum Status {
        DRAFT, // Created but not sent
        SENT, // Sent to the customer
        ACCEPTED, // Customer approved it
        CONVERTED // Converted to an invoice
    }

    @OneToMany(mappedBy = "quote", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<QuoteItem> quoteItems = new ArrayList<>();
}