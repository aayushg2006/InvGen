package com.invoice.generator.controller;

import com.invoice.generator.dto.CreateQuoteDto;
import com.invoice.generator.dto.QuoteSummaryDto;
import com.invoice.generator.model.Invoice;
import com.invoice.generator.model.Quote;
import com.invoice.generator.service.QuoteServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quotes")
public class QuoteController {

    @Autowired
    private QuoteServiceImpl quoteService;

    @PostMapping
    // --- THIS METHOD SIGNATURE IS THE FIX ---
    // It now returns a QuoteSummaryDto instead of the full Quote entity
    public ResponseEntity<QuoteSummaryDto> createQuote(@RequestBody CreateQuoteDto createQuoteDto, @AuthenticationPrincipal UserDetails userDetails) {
        Quote createdQuote = quoteService.createQuote(createQuoteDto, userDetails.getUsername());
        // We map the created quote to a safe DTO before sending it back
        QuoteSummaryDto summaryDto = quoteService.mapQuoteToSummaryDto(createdQuote);
        return new ResponseEntity<>(summaryDto, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<QuoteSummaryDto>> getCurrentUserQuotes(@AuthenticationPrincipal UserDetails userDetails) {
        List<QuoteSummaryDto> quotes = quoteService.getQuotesForUser(userDetails.getUsername());
        return new ResponseEntity<>(quotes, HttpStatus.OK);
    }

    @PostMapping("/{id}/convert")
    public ResponseEntity<Map<String, Long>> convertToInvoice(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        Invoice invoice = quoteService.convertToInvoice(id, userDetails.getUsername());
        return new ResponseEntity<>(Map.of("newInvoiceId", invoice.getId()), HttpStatus.CREATED);
    }
}