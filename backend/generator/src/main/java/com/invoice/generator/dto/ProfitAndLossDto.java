package com.invoice.generator.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProfitAndLossDto {
    private BigDecimal totalRevenue;
    private BigDecimal costOfGoodsSold;
    private BigDecimal grossProfit;
    private BigDecimal totalExpenses;
    private BigDecimal netProfit;
}