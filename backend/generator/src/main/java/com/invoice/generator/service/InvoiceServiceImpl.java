package com.invoice.generator.service;

import com.invoice.generator.dto.*;
import com.invoice.generator.model.*;
import com.invoice.generator.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class InvoiceServiceImpl {

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
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private CustomerCreditRepository customerCreditRepository;
    @Autowired
    private EmailServiceImpl emailService;

    @Transactional
    public Invoice createInvoice(CreateInvoiceDto createInvoiceDto, String username) {
        return createInvoiceWithStatus(createInvoiceDto, username, createInvoiceDto.getStatus());
    }

    @Transactional
    public Invoice createInvoiceWithStatus(CreateInvoiceDto createInvoiceDto, String username, Invoice.Status initialStatus) {
        // ... (this method remains unchanged)
        // Step 1: Validate stock
        for (InvoiceItemDto itemDto : createInvoiceDto.getItems()) {
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            if (product.getQuantityInStock() != null && product.getQuantityInStock() < itemDto.getQuantity()) {
                throw new RuntimeException("Not enough stock for product: " + product.getName() + ". Available: " + product.getQuantityInStock());
            }
        }
        
        // Step 2: Proceed with invoice creation
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Shop shop = user.getShop();
        Customer customer = getOrCreateCustomer(createInvoiceDto, shop);

        Invoice invoice = new Invoice();
        invoice.setShop(shop);
        invoice.setCustomer(customer);
        invoice.setIssueDate(LocalDateTime.now());
        invoice.setInvoiceNumber("INV-" + shop.getId() + "-" + System.currentTimeMillis());
        invoice.setStatus(initialStatus); // Use the provided initial status
        invoice.setTotalAmount(BigDecimal.ZERO);
        invoice.setTotalGst(BigDecimal.ZERO);
        Invoice savedInvoice = invoiceRepository.save(invoice);

        BigDecimal totalAmountWithoutGst = BigDecimal.ZERO;
        BigDecimal totalGst = BigDecimal.ZERO;
        List<InvoiceItem> invoiceItems = new ArrayList<>();
        for (InvoiceItemDto itemDto : createInvoiceDto.getItems()) {
            Product product = productRepository.findById(itemDto.getProductId()).orElseThrow(() -> new RuntimeException("Product not found"));
            InvoiceItem invoiceItem = new InvoiceItem();
            invoiceItem.setProduct(product);
            invoiceItem.setInvoice(savedInvoice);
            invoiceItem.setQuantity(itemDto.getQuantity());
            BigDecimal discountPercentage = itemDto.getDiscountPercentage() != null ? itemDto.getDiscountPercentage() : BigDecimal.ZERO;
            invoiceItem.setDiscountPercentage(discountPercentage);
            BigDecimal originalPrice = product.getSellingPrice();
            BigDecimal discountAmount = originalPrice.multiply(discountPercentage).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            BigDecimal discountedPricePerUnit = originalPrice.subtract(discountAmount);
            if (product.getCostPrice() != null && product.getCostPrice().compareTo(BigDecimal.ZERO) > 0) {
                if (discountedPricePerUnit.compareTo(product.getCostPrice()) < 0) {
                    throw new RuntimeException("Discount for " + product.getName() + " is too high.");
                }
            }
            invoiceItem.setPricePerUnit(discountedPricePerUnit);
            BigDecimal itemSubtotal = discountedPricePerUnit.multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            BigDecimal gstRate = product.getCategory().getGstPercentage().divide(new BigDecimal("100"));
            BigDecimal itemGst = itemSubtotal.multiply(gstRate);
            invoiceItem.setGstAmount(itemGst);
            invoiceItem.setTotalAmount(itemSubtotal.add(itemGst));
            invoiceItems.add(invoiceItem);
            totalAmountWithoutGst = totalAmountWithoutGst.add(itemSubtotal);
            totalGst = totalGst.add(itemGst);
        }
        invoiceItemRepository.saveAll(invoiceItems);
        savedInvoice.setInvoiceItems(invoiceItems);
        savedInvoice.setTotalAmount(totalAmountWithoutGst);
        savedInvoice.setTotalGst(totalGst);

        if (initialStatus == Invoice.Status.AWAITING_PAYMENT) {
            BigDecimal grandTotal = totalAmountWithoutGst.add(totalGst);
            savedInvoice.setAmountPaid(BigDecimal.ZERO);
            savedInvoice.setBalanceDue(grandTotal);
        } else {
            BigDecimal grandTotal = totalAmountWithoutGst.add(totalGst);
            BigDecimal totalPaid = BigDecimal.ZERO;
            if (createInvoiceDto.isApplyCredit()) {
                Optional<CustomerCredit> creditOpt = customerCreditRepository.findByCustomerId(customer.getId());
                if (creditOpt.isPresent()) {
                    CustomerCredit credit = creditOpt.get();
                    if (credit.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal amountToApply = credit.getBalance().min(grandTotal);
                        totalPaid = totalPaid.add(amountToApply);

                        Payment creditPayment = new Payment();
                        creditPayment.setAmount(amountToApply);
                        creditPayment.setPaymentDate(LocalDateTime.now());
                        creditPayment.setPaymentMethod("CREDIT APPLIED");
                        creditPayment.setInvoice(savedInvoice);
                        paymentRepository.save(creditPayment);

                        credit.setBalance(credit.getBalance().subtract(amountToApply));
                        customerCreditRepository.save(credit);
                    }
                }
            }

            if (createInvoiceDto.getStatus() == Invoice.Status.PAID) {
                BigDecimal remainingToPay = grandTotal.subtract(totalPaid);
                if (remainingToPay.compareTo(BigDecimal.ZERO) > 0) {
                    Payment mainPayment = new Payment();
                    mainPayment.setAmount(remainingToPay);
                    mainPayment.setPaymentDate(LocalDateTime.now());
                    mainPayment.setPaymentMethod(createInvoiceDto.getPaymentMethod());
                    mainPayment.setInvoice(savedInvoice);
                    paymentRepository.save(mainPayment);
                }
                savedInvoice.setAmountPaid(grandTotal);
                savedInvoice.setBalanceDue(BigDecimal.ZERO);
                deductStockFromInvoice(savedInvoice);
            } else if (createInvoiceDto.getStatus() == Invoice.Status.PARTIALLY_PAID) {
                if (createInvoiceDto.getInitialAmountPaid() != null && createInvoiceDto.getInitialAmountPaid().compareTo(BigDecimal.ZERO) > 0) {
                    Payment initialPayment = new Payment();
                    initialPayment.setAmount(createInvoiceDto.getInitialAmountPaid());
                    initialPayment.setPaymentDate(LocalDateTime.now());
                    initialPayment.setPaymentMethod(createInvoiceDto.getPaymentMethod());
                    initialPayment.setInvoice(savedInvoice);
                    paymentRepository.save(initialPayment);
                    totalPaid = totalPaid.add(createInvoiceDto.getInitialAmountPaid());
                }
                savedInvoice.setAmountPaid(totalPaid);
                savedInvoice.setBalanceDue(grandTotal.subtract(totalPaid));
            } else { // PENDING
                savedInvoice.setAmountPaid(totalPaid);
                savedInvoice.setBalanceDue(grandTotal.subtract(totalPaid));
                if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
                    savedInvoice.setStatus(Invoice.Status.PARTIALLY_PAID);
                }
            }
        }
        
        return invoiceRepository.save(savedInvoice);
    }
    
    @Transactional
    public Invoice recordPayment(Long invoiceId, PaymentDto paymentDto, String username) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + invoiceId));
        
        if (username != null) {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));
            if (!invoice.getShop().getId().equals(user.getShop().getId())) {
                throw new SecurityException("User not authorized to record payment for this invoice.");
            }
        }
        
        if (invoice.getStatus() == Invoice.Status.AWAITING_PAYMENT || invoice.getStatus() == Invoice.Status.PENDING) {
             invoice.setBalanceDue(invoice.getTotalAmount().add(invoice.getTotalGst()));
             invoice.setAmountPaid(BigDecimal.ZERO);
        }

        Payment newPayment = new Payment();
        newPayment.setAmount(paymentDto.getAmount());
        newPayment.setPaymentMethod(paymentDto.getPaymentMethod());
        newPayment.setPaymentDate(LocalDateTime.now());
        newPayment.setInvoice(invoice);
        Payment savedPayment = paymentRepository.save(newPayment);

        BigDecimal currentBalanceDue = invoice.getBalanceDue();
        BigDecimal overpaymentAmount = paymentDto.getAmount().subtract(currentBalanceDue);

        if (overpaymentAmount.compareTo(BigDecimal.ZERO) > 0) {
            if (paymentDto.getOverpaymentChoice() != null && paymentDto.getOverpaymentChoice().equalsIgnoreCase(PaymentDto.OverpaymentChoice.CREDIT.toString())) {
                CustomerCredit credit = customerCreditRepository.findByCustomerId(invoice.getCustomer().getId())
                        .orElse(new CustomerCredit());
                
                credit.setCustomer(invoice.getCustomer());
                BigDecimal currentBalance = credit.getBalance() != null ? credit.getBalance() : BigDecimal.ZERO;
                credit.setBalance(currentBalance.add(overpaymentAmount));
                customerCreditRepository.save(credit);
            } else {
                Payment refundPayment = new Payment();
                refundPayment.setAmount(overpaymentAmount.negate());
                refundPayment.setPaymentMethod("REFUND");
                refundPayment.setPaymentDate(LocalDateTime.now());
                refundPayment.setInvoice(invoice);
                paymentRepository.save(refundPayment);
            }
        }

        BigDecimal totalPaid = paymentRepository.findAllByInvoice(invoice).stream()
            .map(Payment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        invoice.setAmountPaid(totalPaid);

        BigDecimal grandTotal = invoice.getTotalAmount().add(invoice.getTotalGst());
        BigDecimal newBalance = grandTotal.subtract(totalPaid);
        invoice.setBalanceDue(newBalance);

        if (newBalance.compareTo(BigDecimal.ZERO) <= 0) {
            invoice.setStatus(Invoice.Status.PAID);
            invoice.setBalanceDue(BigDecimal.ZERO);
            deductStockFromInvoice(invoice);
        } else {
            invoice.setStatus(Invoice.Status.PARTIALLY_PAID);
        }

        Invoice updatedInvoice = invoiceRepository.save(invoice);

        // --- THIS IS THE FIX ---
        // We now call the new combined email method regardless of how the payment was made.
        if (paymentDto.isSendReceipt()) {
            try {
                String replyToEmail = invoice.getShop().getUsers().stream()
                        .map(User::getUsername)
                        .findFirst()
                        .orElse("noreply@invgen.com");
                
                // This single method sends both the invoice and the receipt
                emailService.sendPostPaymentEmail(replyToEmail, savedPayment);

            } catch (IOException e) {
                System.err.println("Failed to send post-payment email for payment ID: " + savedPayment.getId() + " - " + e.getMessage());
            }
        }

        return updatedInvoice;
    }
    
    private void deductStockFromInvoice(Invoice invoice) {
        // ... (this method remains unchanged)
        for (InvoiceItem item : invoice.getInvoiceItems()) {
            Product product = item.getProduct();
            if (product.getQuantityInStock() != null) {
                int newStock = product.getQuantityInStock() - item.getQuantity();
                product.setQuantityInStock(newStock);
                productRepository.save(product);
            }
        }
    }

    public List<InvoiceSummaryDto> getInvoicesForUser(String username) {
        // ... (this method remains unchanged)
        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        List<Invoice> invoices = invoiceRepository.findAllByShopOrderByIssueDateDesc(user.getShop());
        return invoices.stream().map(this::mapInvoiceToSummaryDto).collect(Collectors.toList());
    }

    @Transactional
    public void updateInvoiceStatus(Long invoiceId, Invoice.Status status, String username) {
        // ... (this method remains unchanged)
        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow(() -> new RuntimeException("Invoice not found"));
        if (!invoice.getShop().getId().equals(user.getShop().getId())) {
            throw new SecurityException("User not authorized to update this invoice.");
        }
        
        if (invoice.getStatus() != Invoice.Status.PAID && status == Invoice.Status.PAID) {
            deductStockFromInvoice(invoice);
        }
        
        invoice.setStatus(status);
        
        BigDecimal grandTotal = invoice.getTotalAmount().add(invoice.getTotalGst());
        if(status == Invoice.Status.PAID) {
            invoice.setAmountPaid(grandTotal);
            invoice.setBalanceDue(BigDecimal.ZERO);
        } else if (status == Invoice.Status.PENDING || status == Invoice.Status.CANCELLED) {
            BigDecimal totalPaid = paymentRepository.findAllByInvoice(invoice).stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            invoice.setAmountPaid(totalPaid);
            invoice.setBalanceDue(grandTotal.subtract(totalPaid));
        }
        invoiceRepository.save(invoice);
    }

    public InvoiceDetailDto getInvoiceDetails(Long invoiceId, String username) {
        // ... (this method remains unchanged)
        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Invoice invoice = invoiceRepository.findById(invoiceId).orElseThrow(() -> new RuntimeException("Invoice not found"));
        if (!invoice.getShop().getId().equals(user.getShop().getId())) {
            throw new SecurityException("User not authorized to view this invoice.");
        }
        return mapInvoiceToDetailDto(invoice);
    }

    public Invoice getInvoiceById(Long invoiceId) {
        // ... (this method remains unchanged)
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found with id: " + invoiceId));
    }

    private InvoiceSummaryDto mapInvoiceToSummaryDto(Invoice invoice) {
        // ... (this method remains unchanged)
        InvoiceSummaryDto dto = new InvoiceSummaryDto();
        dto.setId(invoice.getId());
        dto.setInvoiceNumber(invoice.getInvoiceNumber());
        dto.setCustomerName(invoice.getCustomer() != null ? invoice.getCustomer().getName() : "N/A");
        dto.setTotalAmount(invoice.getTotalAmount().add(invoice.getTotalGst()));
        dto.setStatus(invoice.getStatus());
        dto.setIssueDate(invoice.getIssueDate());
        dto.setAmountPaid(invoice.getAmountPaid());
        dto.setBalanceDue(invoice.getBalanceDue());
        return dto;
    }
    
    private Customer getOrCreateCustomer(CreateInvoiceDto dto, Shop shop) {
        // ... (this method remains unchanged)
        if (dto.getCustomerId() != null) {
            return customerRepository.findById(dto.getCustomerId()).orElseThrow(() -> new RuntimeException("Customer not found"));
        } else if (dto.getNewCustomerName() != null && !dto.getNewCustomerName().isEmpty()) {
            Customer newCustomer = new Customer();
            newCustomer.setName(dto.getNewCustomerName());
            newCustomer.setPhoneNumber(dto.getNewCustomerPhone());
            newCustomer.setEmail(dto.getNewCustomerEmail());
            newCustomer.setShop(shop);
            return customerRepository.save(newCustomer);
        } else {
            throw new IllegalArgumentException("Customer information is required.");
        }
    }
    
    private InvoiceDetailDto mapInvoiceToDetailDto(Invoice invoice) {
        // ... (this method remains unchanged)
        InvoiceDetailDto dto = new InvoiceDetailDto();
        dto.setInvoiceNumber(invoice.getInvoiceNumber());
        dto.setIssueDate(invoice.getIssueDate());
        dto.setStatus(invoice.getStatus());
        dto.setSubtotal(invoice.getTotalAmount());
        dto.setTotalGst(invoice.getTotalGst());
        dto.setGrandTotal(invoice.getTotalAmount().add(invoice.getTotalGst()));
        if (invoice.getCustomer() != null) {
            dto.setCustomerName(invoice.getCustomer().getName());
            dto.setCustomerPhone(invoice.getCustomer().getPhoneNumber());
            dto.setCustomerEmail(invoice.getCustomer().getEmail());
        }
        if (invoice.getShop() != null) {
            dto.setShopName(invoice.getShop().getShopName());
            dto.setShopAddress(invoice.getShop().getAddress());
            dto.setShopGstin(invoice.getShop().getGstin());
            dto.setShopLogoPath(invoice.getShop().getLogoPath());
        }
        List<InvoiceItemDetailDto> itemDtos = invoiceItemRepository.findAllByInvoice(invoice).stream().map(this::mapInvoiceItemToDetailDto).collect(Collectors.toList());
        dto.setItems(itemDtos);
        dto.setAmountPaid(invoice.getAmountPaid());
        dto.setBalanceDue(invoice.getBalanceDue());

        if (invoice.getPayments() != null) {
            List<PaymentDetailDto> paymentDtos = invoice.getPayments().stream()
                .map(payment -> {
                    PaymentDetailDto paymentDto = new PaymentDetailDto();
                    paymentDto.setId(payment.getId());
                    paymentDto.setAmount(payment.getAmount());
                    paymentDto.setPaymentDate(payment.getPaymentDate());
                    paymentDto.setPaymentMethod(payment.getPaymentMethod());
                    return paymentDto;
                })
                .collect(Collectors.toList());
            dto.setPayments(paymentDtos);
        }

        return dto;
    }

    private InvoiceItemDetailDto mapInvoiceItemToDetailDto(InvoiceItem item) {
        // ... (this method remains unchanged)
        InvoiceItemDetailDto dto = new InvoiceItemDetailDto();
        dto.setProductName(item.getProduct().getName());
        dto.setQuantity(item.getQuantity());
        dto.setPricePerUnit(item.getPricePerUnit());
        dto.setGstAmount(item.getGstAmount());
        dto.setTotalAmount(item.getTotalAmount());
        dto.setDiscountPercentage(item.getDiscountPercentage());
        dto.setOriginalPrice(item.getProduct().getSellingPrice());
        return dto;
    }
}