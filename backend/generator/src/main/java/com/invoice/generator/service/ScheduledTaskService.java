package com.invoice.generator.service;

import com.invoice.generator.dto.CreateInvoiceDto;
import com.invoice.generator.dto.InvoiceItemDto;
import com.invoice.generator.model.Invoice;
import com.invoice.generator.model.Product;
import com.invoice.generator.model.RecurringInvoice;
import com.invoice.generator.model.User;
import com.invoice.generator.repository.ProductRepository;
import com.invoice.generator.repository.RecurringInvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ScheduledTaskService {

    @Autowired
    private RecurringInvoiceRepository recurringInvoiceRepository;

    @Autowired
    private InvoiceServiceImpl invoiceService;

    @Autowired
    private EmailServiceImpl emailService;

    @Autowired
    private ProductRepository productRepository;

    // This scheduled method now simply calls the public processing method.
    @Scheduled(cron = "0 0 1 * * ?")
    public void scheduledInvoiceGeneration() {
        processRecurringInvoices();
    }

    // This is the reusable public method that contains the core logic.
    @Transactional
    public void processRecurringInvoices() {
        System.out.println("Checking for due recurring invoices...");
        LocalDate today = LocalDate.now();
        // Use the updated repository method to find due and overdue profiles.
        List<RecurringInvoice> profilesToProcess = recurringInvoiceRepository.findByNextIssueDateLessThanEqual(today);

        if (profilesToProcess.isEmpty()) {
            System.out.println("No recurring invoices are due for generation.");
            return;
        }

        System.out.println("Found " + profilesToProcess.size() + " recurring profiles to process.");

        for (RecurringInvoice profile : profilesToProcess) {
            // Create a DTO to pass to the existing invoice creation logic.
            CreateInvoiceDto invoiceDto = new CreateInvoiceDto();
            invoiceDto.setCustomerId(profile.getCustomer().getId());

            List<InvoiceItemDto> itemDtos = profile.getItems().stream().map(item -> {
                InvoiceItemDto itemDto = new InvoiceItemDto();
                itemDto.setProductId(item.getProduct().getId());
                itemDto.setQuantity(item.getQuantity());
                itemDto.setDiscountPercentage(item.getDiscountPercentage());
                return itemDto;
            }).collect(Collectors.toList());
            invoiceDto.setItems(itemDtos);

            User user = profile.getShop().getUsers().stream().findFirst()
                    .orElseThrow(() -> new IllegalStateException("No user found for shop id: " + profile.getShop().getId()));

            try {
                // First, create the invoice
                Invoice createdInvoice = invoiceService.createInvoice(invoiceDto, user.getUsername());

                // Second, deduct the stock for each item on the newly created invoice
                for (var item : createdInvoice.getInvoiceItems()) {
                    Product product = item.getProduct();
                    if (product.getQuantityInStock() != null) {
                        int newStock = product.getQuantityInStock() - item.getQuantity();
                        if (newStock < 0) {
                            // This case should ideally be prevented by the validation in createInvoice,
                            // but as a safeguard, we log it.
                            System.err.println("Warning: Stock for product ID " + product.getId() + " went negative after recurring invoice generation.");
                        }
                        product.setQuantityInStock(newStock);
                        productRepository.save(product);
                    }
                }

                // Third, email the invoice
                System.out.println("Automatically sending invoice " + createdInvoice.getInvoiceNumber() + " to " + createdInvoice.getCustomer().getEmail());
                emailService.sendInvoiceEmail(user.getUsername(), createdInvoice, createdInvoice.getCustomer().getEmail(), null);

            } catch (Exception e) {
                System.err.println("Failed to process or send recurring invoice for profile ID: " + profile.getId() + ". Error: " + e.getMessage());
                // Continue to the next profile even if one fails
                continue;
            }

            // Keep advancing the next issue date until it's in the future to catch up on missed days.
            LocalDate nextDate = profile.getNextIssueDate();
            while (!nextDate.isAfter(today)) {
                nextDate = calculateNextIssueDate(nextDate, profile.getFrequency());
            }

            // If the profile has an end date, check if we should continue or delete it.
            if (profile.getEndDate() == null || !nextDate.isAfter(profile.getEndDate())) {
                profile.setNextIssueDate(nextDate);
                recurringInvoiceRepository.save(profile);
            } else {
                recurringInvoiceRepository.delete(profile);
            }
        }
        System.out.println("Finished recurring invoice generation task.");
    }

    private LocalDate calculateNextIssueDate(LocalDate current, RecurringInvoice.Frequency frequency) {
        switch (frequency) {
            case DAILY: return current.plusDays(1);
            case WEEKLY: return current.plusWeeks(1);
            case MONTHLY: return current.plusMonths(1);
            case YEARLY: return current.plusYears(1);
            default: throw new IllegalArgumentException("Unknown frequency: " + frequency);
        }
    }
}