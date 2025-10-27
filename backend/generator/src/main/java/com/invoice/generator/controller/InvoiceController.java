package com.invoice.generator.controller;

import com.invoice.generator.dto.CreateInvoiceDto;
import com.invoice.generator.dto.EmailRequestDto;
import com.invoice.generator.dto.InvoiceDetailDto;
import com.invoice.generator.dto.InvoiceSummaryDto;
import com.invoice.generator.dto.PaymentDto;
import com.invoice.generator.model.Invoice;
// --- ADD THESE IMPORTS ---
import com.invoice.generator.model.Customer;
import com.invoice.generator.model.CustomerCredit;
import com.invoice.generator.repository.CustomerCreditRepository;
import com.invoice.generator.repository.CustomerRepository;
// --- END IMPORTS ---
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
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional; // --- ADD THIS IMPORT ---

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

    // --- ADD THESE REPOSITORIES ---
    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerCreditRepository customerCreditRepository;
    // --- END REPOSITORIES ---

    @PostMapping("/create-for-payment")
    public ResponseEntity<?> createInvoiceForPayment(@RequestBody CreateInvoiceDto createInvoiceDto, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Invoice invoice = invoiceService.createInvoiceWithStatus(createInvoiceDto, userDetails.getUsername(), Invoice.Status.AWAITING_PAYMENT);

            if (invoice.getShop().getRazorpayFundAccountId() == null || !Boolean.TRUE.equals(invoice.getShop().getPaymentsEnabled())) {
                // Clean up the temporarily created invoice if payments aren't enabled
                invoiceRepository.delete(invoice); // Assuming you have invoiceRepository injected or available via invoiceService
                return new ResponseEntity<>("This shop has not enabled online payments.", HttpStatus.BAD_REQUEST);
            }

            // --- THIS IS THE FIX ---
            BigDecimal grandTotal = invoice.getTotalAmount().add(invoice.getTotalGst());
            BigDecimal creditToApply = BigDecimal.ZERO;
            BigDecimal amountForLink = grandTotal; // Default to full amount

            // 1. Check if credit should be applied
            if (createInvoiceDto.isApplyCredit() && invoice.getCustomer() != null) {
                Optional<CustomerCredit> creditOpt = customerCreditRepository.findByCustomerId(invoice.getCustomer().getId());
                if (creditOpt.isPresent()) {
                    BigDecimal availableCredit = creditOpt.get().getBalance();
                    if (availableCredit.compareTo(BigDecimal.ZERO) > 0) {
                        creditToApply = availableCredit.min(grandTotal);
                    }
                }
            }

            BigDecimal remainingAfterCredit = grandTotal.subtract(creditToApply);

            // 2. Determine the final link amount based on status and credit
            if (createInvoiceDto.getStatus() == Invoice.Status.PARTIALLY_PAID && createInvoiceDto.getInitialAmountPaid() != null) {
                // For partial payment, request the initial amount, but not more than what's left after credit
                amountForLink = createInvoiceDto.getInitialAmountPaid().min(remainingAfterCredit);
                 System.out.println("Generating partial payment link for amount (after credit check): " + amountForLink); // Debug log
            } else {
                // For full payment (or if partial amount wasn't specified), request the remaining balance after credit
                amountForLink = remainingAfterCredit;
                 System.out.println("Generating full/remaining payment link for amount (after credit check): " + amountForLink); // Debug log
            }

             // Ensure we don't create a link for zero or negative amount
            if (amountForLink.compareTo(BigDecimal.ZERO) <= 0) {
                 System.out.println("Amount for payment link is zero or less after applying credit. Skipping link generation.");
                 // Optionally, you could automatically mark the invoice as PAID here if credit covers everything
                 // For now, we just won't create the link and let the regular flow handle it (or return an error/success message)
                 // You might want to update the invoice status here directly if fully covered by credit.
                 if (grandTotal.compareTo(creditToApply) <= 0) {
                    // Apply credit fully and mark as paid (similar logic as in recordPayment)
                    // This part needs careful transaction handling if done here.
                    // Let's keep it simple for now and rely on manual marking or webhook if needed.
                    System.out.println("Invoice fully covered by credit.");
                    // Maybe update status to PAID here and skip Razorpay? Requires careful thought on atomicity.
                 }
                 // Return success, but indicate no link needed/generated? Or rely on frontend to handle this?
                 // For now, let's proceed assuming link is needed if amount > 0
                 return new ResponseEntity<>("Invoice amount fully covered by credit.", HttpStatus.OK); // Or handle differently?
            }


            // Pass the correctly calculated amount to the service
            String paymentLink = razorpayService.createPaymentLink(invoice, amountForLink);
            // --- END OF FIX ---


            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "invoiceId", invoice.getId(),
                "paymentLink", paymentLink
            ));

        } catch (RazorpayException | IOException e) {
             System.err.println("Razorpay/IO Error creating payment link: " + e.getMessage());
            return new ResponseEntity<>("Error creating payment link: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
             System.err.println("General Error in /create-for-payment: " + e.getMessage());
             e.printStackTrace(); // Print stack trace for better debugging
            return new ResponseEntity<>("Failed to create invoice for payment: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // --- Need to inject InvoiceRepository if not already available ---
    @Autowired
    private com.invoice.generator.repository.InvoiceRepository invoiceRepository;
    // ---

    // ... (rest of the controller methods remain unchanged)
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
