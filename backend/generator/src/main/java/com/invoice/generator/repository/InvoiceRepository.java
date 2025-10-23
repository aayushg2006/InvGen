package com.invoice.generator.repository;

import com.invoice.generator.model.Invoice;
import com.invoice.generator.model.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findAllByShopOrderByIssueDateDesc(Shop shop);

    @Query("SELECT i FROM Invoice i WHERE (i.status = 'PENDING' OR i.status = 'PARTIALLY_PAID') AND (i.lastReminderSentDate IS NULL OR i.lastReminderSentDate <= :sevenDaysAgo)")
    List<Invoice> findInvoicesForReminder(@Param("sevenDaysAgo") LocalDate sevenDaysAgo);
}