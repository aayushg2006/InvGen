package com.invoice.generator.controller;

import com.invoice.generator.model.Payment;
import com.invoice.generator.service.EmailServiceImpl;
import com.invoice.generator.service.PaymentServiceImpl;
import com.invoice.generator.service.PdfGenerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentServiceImpl paymentService;

    @Autowired
    private PdfGenerationService pdfGenerationService;

    @Autowired
    private EmailServiceImpl emailService;

    @GetMapping("/{paymentId}/receipt")
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable Long paymentId, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Payment payment = paymentService.findByIdAndValidateOwnership(paymentId, userDetails.getUsername());
            byte[] pdfBytes = pdfGenerationService.generatePaymentReceiptPdf(payment);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = "Receipt-PAY-" + payment.getId() + ".pdf";
            headers.setContentDispositionFormData("attachment", filename);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage().getBytes(), HttpStatus.FORBIDDEN);
        }
    }

    @PostMapping("/{paymentId}/email")
    public ResponseEntity<String> emailReceipt(@PathVariable Long paymentId, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Payment payment = paymentService.findByIdAndValidateOwnership(paymentId, userDetails.getUsername());
            emailService.sendPaymentReceiptEmail(userDetails.getUsername(), payment);
            return ResponseEntity.ok("Receipt sent successfully to " + payment.getInvoice().getCustomer().getEmail());
        } catch (IOException e) {
            return new ResponseEntity<>("Failed to send email: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }
}