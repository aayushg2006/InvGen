package com.invoice.generator.service;

import com.invoice.generator.model.Payment;
import com.invoice.generator.model.User;
import com.invoice.generator.repository.PaymentRepository;
import com.invoice.generator.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class PaymentServiceImpl {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    public Payment findByIdAndValidateOwnership(Long paymentId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("Payment not found with id: " + paymentId));

        // Security Check: Ensure the payment belongs to an invoice from the user's shop
        if (!payment.getInvoice().getShop().getId().equals(user.getShop().getId())) {
            throw new SecurityException("User is not authorized to access this payment record.");
        }

        return payment;
    }
}