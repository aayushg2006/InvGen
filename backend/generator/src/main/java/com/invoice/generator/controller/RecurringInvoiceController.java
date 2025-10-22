package com.invoice.generator.controller;

import com.invoice.generator.dto.RecurringInvoiceDto;
import com.invoice.generator.model.RecurringInvoice;
import com.invoice.generator.service.RecurringInvoiceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recurring-invoices")
public class RecurringInvoiceController {

    @Autowired
    private RecurringInvoiceService recurringInvoiceService;

    @PostMapping
    public ResponseEntity<RecurringInvoice> create(@RequestBody RecurringInvoiceDto dto, @AuthenticationPrincipal UserDetails userDetails) {
        RecurringInvoice created = recurringInvoiceService.createRecurringInvoice(dto, userDetails.getUsername());
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }
    
    @GetMapping
    public ResponseEntity<List<RecurringInvoiceDto>> getAll(@AuthenticationPrincipal UserDetails userDetails) {
        List<RecurringInvoiceDto> profiles = recurringInvoiceService.getRecurringInvoicesForUser(userDetails.getUsername());
        return ResponseEntity.ok(profiles);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecurringInvoice> update(@PathVariable Long id, @RequestBody RecurringInvoiceDto dto, @AuthenticationPrincipal UserDetails userDetails) {
        RecurringInvoice updated = recurringInvoiceService.updateRecurringInvoice(id, dto, userDetails.getUsername());
        return ResponseEntity.ok(updated);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        recurringInvoiceService.deleteRecurringInvoice(id, userDetails.getUsername());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}