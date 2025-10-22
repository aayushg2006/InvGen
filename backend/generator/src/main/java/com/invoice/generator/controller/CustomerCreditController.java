package com.invoice.generator.controller;

import com.invoice.generator.model.CustomerCredit;
import com.invoice.generator.repository.CustomerCreditRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/credits")
public class CustomerCreditController {

    @Autowired
    private CustomerCreditRepository customerCreditRepository;

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<Map<String, BigDecimal>> getCreditBalance(@PathVariable Long customerId) {
        BigDecimal balance = customerCreditRepository.findByCustomerId(customerId)
                .map(CustomerCredit::getBalance)
                .orElse(BigDecimal.ZERO);
        
        return ResponseEntity.ok(Map.of("balance", balance));
    }
}