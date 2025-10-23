package com.invoice.generator.service;

import com.invoice.generator.dto.CreateInvoiceDto;
import com.invoice.generator.dto.InvoiceItemDto;
import com.invoice.generator.model.Invoice;
import com.invoice.generator.model.Product;
import com.invoice.generator.model.RecurringInvoice;
import com.invoice.generator.model.User;
import com.invoice.generator.repository.InvoiceRepository;
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
    private InvoiceRepository invoiceRepository;

    @Autowired
    private InvoiceServiceImpl invoiceService;

    @Autowired
    private EmailServiceImpl emailService;

    @Autowired
    private ProductRepository productRepository;

    @Scheduled(cron = "0 0 9 * * ?")
    @Transactional
    public void sendWeeklyPaymentReminders() {
        System.out.println("Checking for pending invoices to send payment reminders...");
        LocalDate sevenDaysAgo = LocalDate.now().minusDays(7);
        List<Invoice> invoicesToSendReminder = invoiceRepository.findInvoicesForReminder(sevenDaysAgo);

        if (invoicesToSendReminder.isEmpty()) {
            System.out.println("No invoices require a payment reminder today.");
            return;
        }

        System.out.println("Found " + invoicesToSendReminder.size() + " invoices needing a payment reminder.");

        for (Invoice invoice : invoicesToSendReminder) {
            try {
                User user = invoice.getShop().getUsers().stream().findFirst()
                    .orElseThrow(() -> new IllegalStateException("No user found for shop id: " + invoice.getShop().getId()));

                System.out.println("Sending reminder for invoice: " + invoice.getInvoiceNumber());
                emailService.sendPaymentReminderEmail(user.getUsername(), invoice);

                invoice.setLastReminderSentDate(LocalDate.now());
                invoiceRepository.save(invoice);

            } catch (Exception e) {
                System.err.println("Failed to send reminder for invoice ID: " + invoice.getId() + ". Error: " + e.getMessage());
            }
        }
        System.out.println("Finished sending payment reminders.");
    }


    @Scheduled(cron = "0 0 1 * * ?")
    public void scheduledInvoiceGeneration() {
        processRecurringInvoices();
    }

    @Transactional
    public void processRecurringInvoices() {
        System.out.println("Checking for due recurring invoices...");
        LocalDate today = LocalDate.now();
        List<RecurringInvoice> profilesToProcess = recurringInvoiceRepository.findByNextIssueDateLessThanEqual(today);

        if (profilesToProcess.isEmpty()) {
            System.out.println("No recurring invoices are due for generation.");
            return;
        }

        System.out.println("Found " + profilesToProcess.size() + " recurring profiles to process.");

        for (RecurringInvoice profile : profilesToProcess) {
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
                Invoice createdInvoice = invoiceService.createInvoice(invoiceDto, user.getUsername());

                for (var item : createdInvoice.getInvoiceItems()) {
                    Product product = item.getProduct();
                    if (product.getQuantityInStock() != null) {
                        int newStock = product.getQuantityInStock() - item.getQuantity();
                        if (newStock < 0) {
                            System.err.println("Warning: Stock for product ID " + product.getId() + " went negative after recurring invoice generation.");
                        }
                        product.setQuantityInStock(newStock);
                        productRepository.save(product);
                    }
                }

                System.out.println("Automatically sending invoice " + createdInvoice.getInvoiceNumber() + " to " + createdInvoice.getCustomer().getEmail());
                emailService.sendInvoiceEmail(user.getUsername(), createdInvoice, createdInvoice.getCustomer().getEmail(), null);

            } catch (Exception e) {
                System.err.println("Failed to process or send recurring invoice for profile ID: " + profile.getId() + ". Error: " + e.getMessage());
                continue;
            }

            LocalDate nextDate = profile.getNextIssueDate();
            while (!nextDate.isAfter(today)) {
                nextDate = calculateNextIssueDate(nextDate, profile.getFrequency());
            }

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