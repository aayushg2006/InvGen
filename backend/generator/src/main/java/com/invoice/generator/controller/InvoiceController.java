package com.invoice.generator.controller;

import com.invoice.generator.dto.CreateInvoiceDto;
import com.invoice.generator.dto.EmailRequestDto;
import com.invoice.generator.dto.InvoiceDetailDto;
import com.invoice.generator.dto.InvoiceSummaryDto;
import com.invoice.generator.dto.PaymentDto;
import com.invoice.generator.model.Invoice;
import com.invoice.generator.service.EmailServiceImpl;
import com.invoice.generator.service.InvoiceServiceImpl;
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
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    @Autowired
    private InvoiceServiceImpl invoiceService;

    @Autowired
    private PdfGenerationService pdfService;

    @Autowired
    private EmailServiceImpl emailService;

    @Autowired
    private RazorpayService razorpayService;

    @PostMapping("/create-for-payment")
    public ResponseEntity<?> createInvoiceForPayment(@RequestBody CreateInvoiceDto createInvoiceDto, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            // Step 1: Create the invoice with a temporary AWAITING_PAYMENT status
            Invoice invoice = invoiceService.createInvoiceWithStatus(createInvoiceDto, userDetails.getUsername(), Invoice.Status.AWAITING_PAYMENT);

            // Step 2: Check if the shop has payments enabled
            if (invoice.getShop().getRazorpayFundAccountId() == null || !Boolean.TRUE.equals(invoice.getShop().getPaymentsEnabled())) {
                return new ResponseEntity<>("This shop has not enabled online payments.", HttpStatus.BAD_REQUEST);
            }

            // Step 3: Call the Razorpay service to create a payment link for the new invoice
            String paymentLink = razorpayService.createPaymentLink(invoice);

            // Step 4: Return the invoice ID and the payment link to the frontend
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "invoiceId", invoice.getId(),
                "paymentLink", paymentLink
            ));

        } catch (RazorpayException | IOException e) {
            return new ResponseEntity<>("Error creating payment link: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            return new ResponseEntity<>("Failed to create invoice for payment: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping
    public ResponseEntity<byte[]> createInvoice(@RequestBody CreateInvoiceDto createInvoiceDto, @AuthenticationPrincipal UserDetails userDetails) {
        Invoice createdInvoice = invoiceService.createInvoice(createInvoiceDto, userDetails.getUsername());
        Invoice freshInvoice = invoiceService.getInvoiceById(createdInvoice.getId());
        byte[] pdfBytes = pdfService.generateInvoicePdf(freshInvoice);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Invoice-" + freshInvoice.getInvoiceNumber() + ".pdf");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.CREATED);
    }

    @PostMapping("/create-and-send")
    public ResponseEntity<String> createAndSendInvoice(@RequestBody CreateInvoiceDto createInvoiceDto, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Invoice createdInvoice = invoiceService.createInvoice(createInvoiceDto, userDetails.getUsername());
            Invoice freshInvoice = invoiceService.getInvoiceById(createdInvoice.getId());
            emailService.sendInvoiceEmail(userDetails.getUsername(), freshInvoice, freshInvoice.getCustomer().getEmail(), null);
            return ResponseEntity.ok("Invoice created and sent successfully to " + freshInvoice.getCustomer().getEmail());
        } catch (IOException e) {
            return new ResponseEntity<>("Invoice was created, but failed to send email: " + e.getMessage(), HttpStatus.MULTI_STATUS);
        } catch (Exception e) {
            return new ResponseEntity<>("Failed to create invoice: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    public ResponseEntity<List<InvoiceSummaryDto>> getCurrentUserInvoices(@AuthenticationPrincipal UserDetails userDetails) {
        List<InvoiceSummaryDto> invoices = invoiceService.getInvoicesForUser(userDetails.getUsername());
        return new ResponseEntity<>(invoices, HttpStatus.OK);
    }

    @PutMapping("/{invoiceId}/status")
    public ResponseEntity<String> updateInvoiceStatus(
            @PathVariable Long invoiceId,
            @RequestParam("status") Invoice.Status status,
            @AuthenticationPrincipal UserDetails userDetails) {
        invoiceService.updateInvoiceStatus(invoiceId, status, userDetails.getUsername());
        return new ResponseEntity<>("Invoice status updated successfully", HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceDetailDto> getInvoiceById(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        InvoiceDetailDto invoiceDetails = invoiceService.getInvoiceDetails(id, userDetails.getUsername());
        return ResponseEntity.ok(invoiceDetails);
    }
    
    @PostMapping("/{invoiceId}/payments")
    public ResponseEntity<String> recordPayment(@PathVariable Long invoiceId, @RequestBody PaymentDto paymentDto, @AuthenticationPrincipal UserDetails userDetails) {
        invoiceService.recordPayment(invoiceId, paymentDto, userDetails.getUsername());
        return new ResponseEntity<>("Payment recorded successfully", HttpStatus.OK);    
    }

    @PostMapping("/{invoiceId}/email")
    public ResponseEntity<String> sendInvoiceByEmail(
            @PathVariable Long invoiceId,
            @RequestBody EmailRequestDto emailRequestDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            InvoiceDetailDto invoiceDetails = invoiceService.getInvoiceDetails(invoiceId, userDetails.getUsername());
            Invoice invoice = invoiceService.getInvoiceById(invoiceId);
            
            String recipientEmail = emailRequestDto.getTo();
            if (recipientEmail == null || recipientEmail.isEmpty()) {
                recipientEmail = invoiceDetails.getCustomerEmail();
            }

            emailService.sendInvoiceEmail(userDetails.getUsername(), invoice, recipientEmail, emailRequestDto.getCustomMessage());
            return new ResponseEntity<>("Invoice sent successfully to " + recipientEmail, HttpStatus.OK);
        } catch (IOException e) {
            return new ResponseEntity<>("Failed to send email: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (RuntimeException e) {
             return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }
}