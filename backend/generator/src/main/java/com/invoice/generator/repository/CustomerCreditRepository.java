package com.invoice.generator.repository;

import com.invoice.generator.model.CustomerCredit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerCreditRepository extends JpaRepository<CustomerCredit, Long> {
    Optional<CustomerCredit> findByCustomerId(Long customerId);
}