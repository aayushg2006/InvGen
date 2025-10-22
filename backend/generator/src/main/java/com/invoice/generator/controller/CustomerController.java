package com.invoice.generator.controller;

import com.invoice.generator.dto.CustomerDto;
import com.invoice.generator.model.Customer;
import com.invoice.generator.service.CustomerServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    @Autowired
    private CustomerServiceImpl customerService;

    @PostMapping
    public ResponseEntity<String> createCustomer(@RequestBody CustomerDto customerDto, @AuthenticationPrincipal UserDetails userDetails) {
        customerService.createCustomer(customerDto, userDetails.getUsername());
        return new ResponseEntity<>("Customer created successfully", HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<CustomerDto>> getCurrentUserCustomers(@AuthenticationPrincipal UserDetails userDetails) {
        List<CustomerDto> customers = customerService.getCustomersByUser(userDetails.getUsername());
        return new ResponseEntity<>(customers, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerDto> updateCustomer(@PathVariable Long id, @RequestBody CustomerDto customerDto, @AuthenticationPrincipal UserDetails userDetails) { // Return CustomerDto
        CustomerDto updatedCustomer = customerService.updateCustomer(id, customerDto, userDetails.getUsername()); // Receive CustomerDto
        return new ResponseEntity<>(updatedCustomer, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteCustomer(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        customerService.deleteCustomer(id, userDetails.getUsername());
        return new ResponseEntity<>("Customer deleted successfully", HttpStatus.OK);
    }
}