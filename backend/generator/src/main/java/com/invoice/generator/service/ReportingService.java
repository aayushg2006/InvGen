package com.invoice.generator.service;

import com.invoice.generator.dto.PaymentSummaryDto;
import com.invoice.generator.model.User;
import com.invoice.generator.repository.ReportingRepository;
import com.invoice.generator.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReportingService {

    @Autowired
    private ReportingRepository reportingRepository;

    @Autowired
    private UserRepository userRepository;

    public List<PaymentSummaryDto> getPaymentSummaryByMethod(String username, LocalDateTime startDate, LocalDateTime endDate) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return reportingRepository.getPaymentSummaryByMethod(user.getShop(), startDate, endDate);
    }
}