package com.invoice.generator.repository;

import com.invoice.generator.model.RecurringInvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RecurringInvoiceItemRepository extends JpaRepository<RecurringInvoiceItem, Long> {
}