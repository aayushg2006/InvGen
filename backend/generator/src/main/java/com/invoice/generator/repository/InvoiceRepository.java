package com.invoice.generator.repository;

import com.invoice.generator.model.Invoice;
import com.invoice.generator.model.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    
    // This method name tells Spring to sort the results by the 'issueDate' field in descending order.
    List<Invoice> findAllByShopOrderByIssueDateDesc(Shop shop);
}