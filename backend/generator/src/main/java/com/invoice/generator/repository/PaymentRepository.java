package com.invoice.generator.repository;

import com.invoice.generator.model.Invoice; // <-- ADD THIS IMPORT
import com.invoice.generator.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List; // <-- ADD THIS IMPORT

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // ADD THIS METHOD
    List<Payment> findAllByInvoice(Invoice invoice);
}