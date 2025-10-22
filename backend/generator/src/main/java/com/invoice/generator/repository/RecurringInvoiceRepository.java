package com.invoice.generator.repository;

import com.invoice.generator.model.RecurringInvoice;
import com.invoice.generator.model.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RecurringInvoiceRepository extends JpaRepository<RecurringInvoice, Long> {
    // --- RENAME AND UPDATE THIS METHOD ---
    List<RecurringInvoice> findByNextIssueDateLessThanEqual(LocalDate date);
    
    List<RecurringInvoice> findAllByShop(Shop shop);
}