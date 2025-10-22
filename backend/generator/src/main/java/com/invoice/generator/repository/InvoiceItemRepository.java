package com.invoice.generator.repository;

import com.invoice.generator.model.Invoice;
import com.invoice.generator.model.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List; // <-- ADD THIS IMPORT

@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {

    // This method requires the 'List' import
    List<InvoiceItem> findAllByInvoice(Invoice invoice);
    
}