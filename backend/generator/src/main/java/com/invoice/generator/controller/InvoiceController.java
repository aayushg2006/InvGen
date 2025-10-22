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

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    @Autowired
    private InvoiceServiceImpl invoiceService;

    @Autowired
    private PdfGenerationService pdfService;

    @Autowired
    private EmailServiceImpl emailService;

    @PostMapping
    public ResponseEntity<byte[]> createInvoice(@RequestBody CreateInvoiceDto createInvoiceDto, @AuthenticationPrincipal UserDetails userDetails) {
        // Step 1: Create the invoice in the database as before
        Invoice createdInvoice = invoiceService.createInvoice(createInvoiceDto, userDetails.getUsername());
        
        // Step 2: Re-fetch the invoice to ensure all collections (like payments) are fully loaded
        Invoice freshInvoice = invoiceService.getInvoiceById(createdInvoice.getId());
        
        // Step 3: Generate the PDF using the fresh, complete invoice object
        byte[] pdfBytes = pdfService.generateInvoicePdf(freshInvoice);

        // Step 4: Set up HTTP headers to tell the browser to download the file
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Invoice-" + freshInvoice.getInvoiceNumber() + ".pdf");

        // Step 5: Return the PDF file bytes as the response
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.CREATED);
    }

    // --- NEW ENDPOINT FOR "SAVE AND EMAIL" ---
    @PostMapping("/create-and-send")
    public ResponseEntity<String> createAndSendInvoice(@RequestBody CreateInvoiceDto createInvoiceDto, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            // Step 1: Create the invoice in the database
            Invoice createdInvoice = invoiceService.createInvoice(createInvoiceDto, userDetails.getUsername());
            
            // Step 2: Re-fetch the complete invoice object to ensure all data is loaded
            Invoice freshInvoice = invoiceService.getInvoiceById(createdInvoice.getId());

            // Step 3: Email the invoice
            emailService.sendInvoiceEmail(userDetails.getUsername(), freshInvoice, freshInvoice.getCustomer().getEmail(), null); // Sending with default message

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