package com.invoice.generator.service;

import com.invoice.generator.dto.DashboardStatsDto;
import com.invoice.generator.model.Invoice;
import com.invoice.generator.model.User;
import com.invoice.generator.repository.InvoiceRepository;
import com.invoice.generator.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode; // --- THIS IS THE FIX ---
import java.util.List;

@Service
public class DashboardServiceImpl {

    @Autowired
    private InvoiceRepository invoiceRepository;
    @Autowired
    private UserRepository userRepository;

    public DashboardStatsDto getDashboardStats(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<Invoice> allInvoices = invoiceRepository.findAllByShopOrderByIssueDateDesc(user.getShop());
        
        DashboardStatsDto stats = new DashboardStatsDto();

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalGstPayable = BigDecimal.ZERO;
        long invoicesDue = 0;
        long invoicesPaid = 0;
        long invoicesPartiallyPaid = 0;

        for (Invoice invoice : allInvoices) {
            if (invoice.getStatus() == Invoice.Status.PAID) {
                invoicesPaid++;
                if(invoice.getAmountPaid() != null) {
                    BigDecimal grandTotal = invoice.getTotalAmount().add(invoice.getTotalGst());
                    if (grandTotal.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal paidRatio = invoice.getAmountPaid().divide(grandTotal, 4, RoundingMode.HALF_UP);
                        totalRevenue = totalRevenue.add(invoice.getTotalAmount().multiply(paidRatio));
                    }
                }
                totalGstPayable = totalGstPayable.add(invoice.getTotalGst());
            } else if (invoice.getStatus() == Invoice.Status.PENDING) {
                invoicesDue++;
            } else if (invoice.getStatus() == Invoice.Status.PARTIALLY_PAID) {
                invoicesPartiallyPaid++;
                 if(invoice.getAmountPaid() != null) {
                    BigDecimal grandTotal = invoice.getTotalAmount().add(invoice.getTotalGst());
                    if (grandTotal.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal paidRatio = invoice.getAmountPaid().divide(grandTotal, 4, RoundingMode.HALF_UP);
                        totalRevenue = totalRevenue.add(invoice.getTotalAmount().multiply(paidRatio));
                    }
                }
            }
        }
        
        stats.setTotalRevenue(totalRevenue);
        stats.setInvoicesPaid(invoicesPaid);
        stats.setInvoicesDue(invoicesDue);
        stats.setTotalGstPayable(totalGstPayable);
        stats.setInvoicesPartiallyPaid(invoicesPartiallyPaid);

        return stats;
    }
}