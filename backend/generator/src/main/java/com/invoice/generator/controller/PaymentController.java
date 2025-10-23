package com.invoice.generator.controller;

import com.invoice.generator.dto.PaymentDto;
import com.invoice.generator.model.Invoice;
import com.invoice.generator.model.Payment;
import com.invoice.generator.service.EmailServiceImpl;
import com.invoice.generator.service.InvoiceServiceImpl;
import com.invoice.generator.service.PaymentServiceImpl;
import com.invoice.generator.service.PdfGenerationService;
import com.invoice.generator.service.RazorpayService;
import com.razorpay.RazorpayException;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;


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

    @PostMapping("/razorpay-webhook")
    public ResponseEntity<String> handleRazorpayWebhook(@RequestBody String payload, @RequestHeader("X-Razorpay-Signature") String signature) {
        System.out.println("--- Razorpay Webhook Received ---");

        if (!razorpayService.verifyWebhookSignature(payload, signature)) {
            System.err.println("Webhook signature verification failed.");
            return new ResponseEntity<>("Invalid signature", HttpStatus.BAD_REQUEST);
        }

        try {
            JSONObject payloadJson = new JSONObject(payload);
            String event = payloadJson.getString("event");

            if ("payment_link.paid".equals(event)) {
                // --- THIS IS THE FIX ---
                // The 'notes' are inside the 'payment' entity, not the 'payment_link' entity.
                JSONObject paymentEntity = payloadJson.getJSONObject("payload").getJSONObject("payment").getJSONObject("entity");
                JSONObject notes = paymentEntity.getJSONObject("notes");
                
                long invoiceId = Long.parseLong(notes.getString("invoice_id"));
                
                // Get the amount that was actually paid
                BigDecimal amountPaid = new BigDecimal(paymentEntity.getInt("amount")).divide(new BigDecimal(100));
                
                PaymentDto paymentDto = new PaymentDto();
                paymentDto.setAmount(amountPaid);
                paymentDto.setPaymentMethod("RAZORPAY_ONLINE");
                paymentDto.setSendReceipt(true); 
                
                invoiceService.recordPayment(invoiceId, paymentDto, null);

                System.out.println("Successfully processed payment for invoice ID: " + invoiceId);
            }

        } catch (Exception e) {
            System.err.println("Error processing webhook: " + e.getMessage());
            e.printStackTrace(); // Print stack trace for better debugging
        }

        return new ResponseEntity<>("Webhook processed", HttpStatus.OK);
    }


    @PostMapping("/create-link/{invoiceId}")
    public ResponseEntity<?> createPaymentLink(@PathVariable Long invoiceId, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Invoice invoice = invoiceService.getInvoiceById(invoiceId);
            if (!invoice.getShop().getUsers().stream().anyMatch(u -> u.getUsername().equals(userDetails.getUsername()))) {
                return new ResponseEntity<>("You are not authorized to access this invoice.", HttpStatus.FORBIDDEN);
            }

            if (invoice.getShop().getRazorpayFundAccountId() == null || !Boolean.TRUE.equals(invoice.getShop().getPaymentsEnabled())) {
                return new ResponseEntity<>("This shop has not enabled online payments.", HttpStatus.BAD_REQUEST);
            }
            
            if (invoice.getBalanceDue() == null || invoice.getBalanceDue().compareTo(BigDecimal.ZERO) <= 0) {
                return new ResponseEntity<>("This invoice is already fully paid.", HttpStatus.BAD_REQUEST);
            }

            String paymentLink = razorpayService.createPaymentLink(invoice);

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