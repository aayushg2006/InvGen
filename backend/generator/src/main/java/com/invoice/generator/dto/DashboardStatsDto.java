package com.invoice.generator.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DashboardStatsDto {
    private BigDecimal totalRevenue;
    private long invoicesDue;
    private long invoicesPaid;
    private BigDecimal totalGstPayable;

    // --- ADDED FIELD ---
    private long invoicesPartiallyPaid;
}