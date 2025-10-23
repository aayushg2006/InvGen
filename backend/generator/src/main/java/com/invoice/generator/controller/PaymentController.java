package com.invoice.generator.controller;

import com.invoice.generator.model.Invoice;
import com.invoice.generator.model.Payment;
import com.invoice.generator.service.EmailServiceImpl;
import com.invoice.generator.service.InvoiceServiceImpl;
import com.invoice.generator.service.PaymentServiceImpl;
import com.invoice.generator.service.PdfGenerationService;
import com.invoice.generator.service.RazorpayService;
import com.razorpay.RazorpayException;

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
import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentServiceImpl paymentService;

    @Autowired
    private PdfGenerationService pdfGenerationService;

    @Autowired
    private EmailServiceImpl emailService;
    
    @Autowired
    private InvoiceServiceImpl invoiceService;
    
    @Autowired
    private RazorpayService razorpayService;

    @PostMapping("/create-link/{invoiceId}")
    public ResponseEntity<?> createPaymentLink(@PathVariable Long invoiceId, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            // 1. Fetch the invoice and validate ownership
            Invoice invoice = invoiceService.getInvoiceById(invoiceId);
            if (!invoice.getShop().getUsers().stream().anyMatch(u -> u.getUsername().equals(userDetails.getUsername()))) {
                return new ResponseEntity<>("You are not authorized to access this invoice.", HttpStatus.FORBIDDEN);
            }

            // 2. Use getPaymentsEnabled() and a null-safe check
            if (invoice.getShop().getRazorpayFundAccountId() == null || !Boolean.TRUE.equals(invoice.getShop().getPaymentsEnabled())) {
                return new ResponseEntity<>("This shop has not enabled online payments.", HttpStatus.BAD_REQUEST);
            }
            
            // 3. Check if there is a balance to be paid
            if (invoice.getBalanceDue() == null || invoice.getBalanceDue().compareTo(BigDecimal.ZERO) <= 0) {
                return new ResponseEntity<>("This invoice is already fully paid.", HttpStatus.BAD_REQUEST);
            }

            // 4. Call the service to create the link
            String paymentLink = razorpayService.createPaymentLink(invoice);

            // 5. Return the link to the frontend
            return ResponseEntity.ok(Map.of("paymentLink", paymentLink));

        } catch (RazorpayException | IOException e) {
            return new ResponseEntity<>("Error creating payment link: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

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