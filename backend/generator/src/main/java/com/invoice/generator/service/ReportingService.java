package com.invoice.generator.service;

import com.invoice.generator.dto.*;
import com.invoice.generator.model.User;
import com.invoice.generator.repository.ExpenseRepository;
import com.invoice.generator.repository.ReportingRepository;
import com.invoice.generator.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReportingService {

    @Autowired
    private ReportingRepository reportingRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private UserRepository userRepository;

    public List<PaymentSummaryDto> getPaymentSummaryByMethod(String username, LocalDateTime startDate, LocalDateTime endDate) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return reportingRepository.getPaymentSummaryByMethod(user.getShop(), startDate, endDate);
    }

    public List<RevenueByCustomerDto> getRevenueByCustomer(String username, LocalDateTime startDate, LocalDateTime endDate) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return reportingRepository.getRevenueByCustomer(user.getShop(), startDate, endDate);
    }

    public List<SalesByProductDto> getSalesByProduct(String username, LocalDateTime startDate, LocalDateTime endDate) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return reportingRepository.getSalesByProduct(user.getShop(), startDate, endDate);
    }

    public ProfitAndLossDto generateProfitAndLoss(String username, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        BigDecimal totalRevenue = reportingRepository.sumRevenueByShopAndDateRange(user.getShop(), startDateTime, endDateTime);
        totalRevenue = (totalRevenue == null) ? BigDecimal.ZERO : totalRevenue;

        BigDecimal cogs = reportingRepository.sumCostOfGoodsSoldByShopAndDateRange(user.getShop(), startDateTime, endDateTime);
        cogs = (cogs == null) ? BigDecimal.ZERO : cogs;
        
        LocalDate startDate = startDateTime.toLocalDate();
        LocalDate endDate = endDateTime.toLocalDate();
        BigDecimal totalExpenses = expenseRepository.sumExpensesByShopAndDateRange(user.getShop(), startDate, endDate);
        totalExpenses = (totalExpenses == null) ? BigDecimal.ZERO : totalExpenses;

        BigDecimal grossProfit = totalRevenue.subtract(cogs);
        BigDecimal netProfit = grossProfit.subtract(totalExpenses);

        ProfitAndLossDto pnl = new ProfitAndLossDto();
        pnl.setTotalRevenue(totalRevenue);
        pnl.setCostOfGoodsSold(cogs);
        pnl.setGrossProfit(grossProfit);
        pnl.setTotalExpenses(totalExpenses);
        pnl.setNetProfit(netProfit);

        return pnl;
    }
}