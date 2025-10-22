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

    @OneToMany(mappedBy = "shop", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<User> users = new ArrayList<>();
}