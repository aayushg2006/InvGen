package com.invoice.generator.controller;

import com.invoice.generator.dto.PaymentSummaryDto;
import com.invoice.generator.service.ReportingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportingController {

    @Autowired
    private ReportingService reportingService;

    @GetMapping("/payment-summary")
    public ResponseEntity<List<PaymentSummaryDto>> getPaymentSummary(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        List<PaymentSummaryDto> summary = reportingService.getPaymentSummaryByMethod(userDetails.getUsername(), startDate, endDate);
        return ResponseEntity.ok(summary);
    }
}