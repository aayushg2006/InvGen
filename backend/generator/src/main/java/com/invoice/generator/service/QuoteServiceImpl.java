package com.invoice.generator.service;

import com.invoice.generator.dto.CreateQuoteDto;
import com.invoice.generator.dto.InvoiceItemDto;
import com.invoice.generator.dto.QuoteSummaryDto;
import com.invoice.generator.model.*;
import com.invoice.generator.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class QuoteServiceImpl {

    @Autowired
    private QuoteRepository quoteRepository;
    @Autowired
    private QuoteItemRepository quoteItemRepository;
    @Autowired
    private InvoiceRepository invoiceRepository;
    @Autowired
    private InvoiceItemRepository invoiceItemRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CustomerRepository customerRepository;

    @Transactional
    public Quote createQuote(CreateQuoteDto createQuoteDto, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Shop shop = user.getShop();

        Customer customer = getOrCreateCustomer(createQuoteDto.getCustomerId(), createQuoteDto.getNewCustomerName(), createQuoteDto.getNewCustomerPhone(), createQuoteDto.getNewCustomerEmail(), shop);

        Quote quote = new Quote();
        quote.setShop(shop);
        quote.setCustomer(customer);
        quote.setIssueDate(LocalDateTime.now());
        quote.setStatus(Quote.Status.DRAFT);
        quote.setQuoteNumber("QUO-" + shop.getId() + "-" + System.currentTimeMillis());

        // --- THIS IS THE FIX ---
        // Initialize totals to zero before the first save
        quote.setTotalAmount(BigDecimal.ZERO);
        quote.setTotalGst(BigDecimal.ZERO);
        // --- END OF FIX ---

        Quote savedQuote = quoteRepository.save(quote);

        BigDecimal totalAmountWithoutGst = BigDecimal.ZERO;
        BigDecimal totalGst = BigDecimal.ZERO;

        List<QuoteItem> quoteItems = new ArrayList<>();
        for (InvoiceItemDto itemDto : createQuoteDto.getItems()) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found with id: " + itemDto.getProductId()));

            QuoteItem quoteItem = new QuoteItem();
            quoteItem.setProduct(product);
            quoteItem.setQuote(savedQuote);
            quoteItem.setQuantity(itemDto.getQuantity());

            BigDecimal discountPercentage = itemDto.getDiscountPercentage() != null ? itemDto.getDiscountPercentage() : BigDecimal.ZERO;
            quoteItem.setDiscountPercentage(discountPercentage);

            BigDecimal originalPrice = product.getSellingPrice();
            BigDecimal discountAmount = originalPrice.multiply(discountPercentage).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            BigDecimal discountedPricePerUnit = originalPrice.subtract(discountAmount);

            if (product.getCostPrice() != null && product.getCostPrice().compareTo(BigDecimal.ZERO) > 0) {
                if (discountedPricePerUnit.compareTo(product.getCostPrice()) < 0) {
                    throw new RuntimeException("Discount on product '" + product.getName() + "' is too high. Price cannot be lower than the cost price.");
                }
            }

            quoteItem.setPricePerUnit(discountedPricePerUnit);

            BigDecimal itemSubtotal = discountedPricePerUnit.multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            BigDecimal gstRate = product.getCategory().getGstPercentage().divide(new BigDecimal("100"));
            BigDecimal itemGst = itemSubtotal.multiply(gstRate);

            quoteItem.setGstAmount(itemGst);
            quoteItem.setTotalAmount(itemSubtotal.add(itemGst));

            quoteItems.add(quoteItem);

            totalAmountWithoutGst = totalAmountWithoutGst.add(itemSubtotal);
            totalGst = totalGst.add(itemGst);
        }

        quoteItemRepository.saveAll(quoteItems);

        savedQuote.setQuoteItems(quoteItems);
        savedQuote.setTotalAmount(totalAmountWithoutGst);
        savedQuote.setTotalGst(totalGst);

        return quoteRepository.save(savedQuote);
    }

    public List<QuoteSummaryDto> getQuotesForUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<Quote> quotes = quoteRepository.findAllByShopOrderByIssueDateDesc(user.getShop());

        return quotes.stream()
                .map(this::mapQuoteToSummaryDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public Invoice convertToInvoice(Long quoteId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Quote quote = quoteRepository.findById(quoteId)
                .orElseThrow(() -> new RuntimeException("Quote not found with id: " + quoteId));

        if (!quote.getShop().getId().equals(user.getShop().getId())) {
            throw new SecurityException("User is not authorized to convert this quote.");
        }
        if (quote.getStatus() == Quote.Status.CONVERTED) {
            throw new IllegalStateException("This quote has already been converted to an invoice.");
        }

        Invoice invoice = new Invoice();
        invoice.setShop(quote.getShop());
        invoice.setCustomer(quote.getCustomer());
        invoice.setIssueDate(LocalDateTime.now());
        invoice.setStatus(Invoice.Status.PENDING);
        invoice.setTotalAmount(quote.getTotalAmount());
        invoice.setTotalGst(quote.getTotalGst());
        invoice.setInvoiceNumber("INV-" + quote.getShop().getId() + "-" + System.currentTimeMillis());
        Invoice savedInvoice = invoiceRepository.save(invoice);

        List<InvoiceItem> invoiceItems = new ArrayList<>();
        for (QuoteItem quoteItem : quote.getQuoteItems()) {
            InvoiceItem invoiceItem = new InvoiceItem();
            invoiceItem.setInvoice(savedInvoice);
            invoiceItem.setProduct(quoteItem.getProduct());
            invoiceItem.setQuantity(quoteItem.getQuantity());
            invoiceItem.setPricePerUnit(quoteItem.getPricePerUnit());
            invoiceItem.setDiscountPercentage(quoteItem.getDiscountPercentage());
            invoiceItem.setGstAmount(quoteItem.getGstAmount());
            invoiceItem.setTotalAmount(quoteItem.getTotalAmount());
            invoiceItems.add(invoiceItem);
        }
        invoiceItemRepository.saveAll(invoiceItems);
        savedInvoice.setInvoiceItems(invoiceItems);

        quote.setStatus(Quote.Status.CONVERTED);
        quote.setInvoice(savedInvoice);
        quoteRepository.save(quote);

        return savedInvoice;
    }

    private Customer getOrCreateCustomer(Long customerId, String newName, String newPhone, String newEmail, Shop shop) {
        if (customerId != null) {
            return customerRepository.findById(customerId)
                    .orElseThrow(() -> new RuntimeException("Customer not found with id: " + customerId));
        } else if (newName != null && !newName.isEmpty()) {
            Customer newCustomer = new Customer();
            newCustomer.setName(newName);
            newCustomer.setPhoneNumber(newPhone);
            newCustomer.setEmail(newEmail);
            newCustomer.setShop(shop);
            return customerRepository.save(newCustomer);
        } else {
            throw new IllegalArgumentException("Either an existing customer ID or a new customer name must be provided.");
        }
    }

    public QuoteSummaryDto mapQuoteToSummaryDto(Quote quote) {
        QuoteSummaryDto dto = new QuoteSummaryDto();
        dto.setId(quote.getId());
        dto.setQuoteNumber(quote.getQuoteNumber());
        if (quote.getCustomer() != null) {
            dto.setCustomerName(quote.getCustomer().getName());
        }
        dto.setTotalAmount(quote.getTotalAmount().add(quote.getTotalGst()));
        dto.setStatus(quote.getStatus());
        dto.setIssueDate(quote.getIssueDate());
        return dto;
    }
}