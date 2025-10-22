package com.invoice.generator.service;

import com.invoice.generator.dto.CustomerDto;
import com.invoice.generator.model.Customer;
import com.invoice.generator.model.CustomerCredit; // <-- ADD THIS IMPORT
import com.invoice.generator.model.User;
import com.invoice.generator.repository.CustomerCreditRepository; // <-- ADD THIS IMPORT
import com.invoice.generator.repository.CustomerRepository;
import com.invoice.generator.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal; // <-- ADD THIS IMPORT
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CustomerServiceImpl {

    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private UserRepository userRepository;
    
    // INJECT THE NEW REPOSITORY
    @Autowired
    private CustomerCreditRepository customerCreditRepository;

    public Customer createCustomer(CustomerDto customerDto, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        Customer customer = new Customer();
        customer.setName(customerDto.getName());
        customer.setEmail(customerDto.getEmail());
        customer.setPhoneNumber(customerDto.getPhoneNumber());
        customer.setShop(user.getShop());
        
        return customerRepository.save(customer);
    }

    // THIS METHOD IS MODIFIED
    public List<CustomerDto> getCustomersByUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        return customerRepository.findAllByShop(user.getShop())
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }
    
    @Transactional
    public CustomerDto updateCustomer(Long customerId, CustomerDto customerDto, String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Customer customerToUpdate = customerRepository.findById(customerId).orElseThrow(() -> new RuntimeException("Customer not found"));

        if (!customerToUpdate.getShop().getId().equals(user.getShop().getId())) {
            throw new SecurityException("User not authorized to update this customer.");
        }

        customerToUpdate.setName(customerDto.getName());
        customerToUpdate.setPhoneNumber(customerDto.getPhoneNumber());
        customerToUpdate.setEmail(customerDto.getEmail());
        Customer savedCustomer = customerRepository.save(customerToUpdate);
        return mapToDto(savedCustomer);
    }


    @Transactional
    public void deleteCustomer(Long customerId, String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Customer customerToDelete = customerRepository.findById(customerId).orElseThrow(() -> new RuntimeException("Customer not found"));

        if (!customerToDelete.getShop().getId().equals(user.getShop().getId())) {
            throw new SecurityException("User not authorized to delete this customer.");
        }
        
        customerRepository.delete(customerToDelete);
    }

    // THIS METHOD IS MODIFIED
    private CustomerDto mapToDto(Customer customer) {
        CustomerDto dto = new CustomerDto();
        dto.setId(customer.getId());
        dto.setName(customer.getName());
        dto.setEmail(customer.getEmail());
        dto.setPhoneNumber(customer.getPhoneNumber());
        
        // Fetch and set the credit balance
        BigDecimal balance = customerCreditRepository.findByCustomerId(customer.getId())
                .map(CustomerCredit::getBalance)
                .orElse(BigDecimal.ZERO);
        dto.setCreditBalance(balance);
        
        return dto;
    }
}