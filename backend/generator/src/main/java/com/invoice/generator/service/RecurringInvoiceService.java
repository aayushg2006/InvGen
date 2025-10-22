package com.invoice.generator.service;

import com.invoice.generator.dto.InvoiceItemDto;
import com.invoice.generator.dto.RecurringInvoiceDto;
import com.invoice.generator.model.*;
import com.invoice.generator.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecurringInvoiceService {

    @Autowired
    private RecurringInvoiceRepository recurringInvoiceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Transactional
    public RecurringInvoice createRecurringInvoice(RecurringInvoiceDto dto, String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Customer customer = customerRepository.findById(dto.getCustomerId()).orElseThrow(() -> new RuntimeException("Customer not found"));

        RecurringInvoice profile = new RecurringInvoice();
        profile.setCustomer(customer);
        profile.setFrequency(dto.getFrequency());
        profile.setStartDate(dto.getStartDate());
        profile.setEndDate(dto.getEndDate());
        profile.setNextIssueDate(dto.getStartDate());
        profile.setAutoSendEmail(dto.isAutoSendEmail());
        profile.setShop(user.getShop());

        List<RecurringInvoiceItem> items = dto.getItems().stream().map(itemDto -> {
            Product product = productRepository.findById(itemDto.getProductId()).orElseThrow(() -> new RuntimeException("Product not found"));
            RecurringInvoiceItem item = new RecurringInvoiceItem();
            item.setProduct(product);
            item.setQuantity(itemDto.getQuantity());
            item.setDiscountPercentage(itemDto.getDiscountPercentage());
            item.setRecurringInvoice(profile);
            return item;
        }).collect(Collectors.toList());
        profile.setItems(items);

        return recurringInvoiceRepository.save(profile);
    }

    @Transactional
    public RecurringInvoice updateRecurringInvoice(Long profileId, RecurringInvoiceDto dto, String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        RecurringInvoice profile = recurringInvoiceRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Recurring profile not found"));

        if (!profile.getShop().getId().equals(user.getShop().getId())) {
            throw new SecurityException("User not authorized to update this profile.");
        }

        Customer customer = customerRepository.findById(dto.getCustomerId()).orElseThrow(() -> new RuntimeException("Customer not found"));

        profile.setCustomer(customer);
        profile.setFrequency(dto.getFrequency());
        profile.setStartDate(dto.getStartDate());
        profile.setEndDate(dto.getEndDate());
        profile.setNextIssueDate(dto.getStartDate());
        profile.setAutoSendEmail(dto.isAutoSendEmail());

        profile.getItems().clear();
        List<RecurringInvoiceItem> items = dto.getItems().stream().map(itemDto -> {
            Product product = productRepository.findById(itemDto.getProductId()).orElseThrow(() -> new RuntimeException("Product not found"));
            RecurringInvoiceItem item = new RecurringInvoiceItem();
            item.setProduct(product);
            item.setQuantity(itemDto.getQuantity());
            item.setDiscountPercentage(itemDto.getDiscountPercentage());
            item.setRecurringInvoice(profile);
            return item;
        }).collect(Collectors.toList());
        profile.getItems().addAll(items);

        return recurringInvoiceRepository.save(profile);
    }

    @Transactional
    public void deleteRecurringInvoice(Long profileId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        RecurringInvoice profileToDelete = recurringInvoiceRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("Recurring profile not found with id: " + profileId));

        if (!profileToDelete.getShop().getId().equals(user.getShop().getId())) {
            throw new SecurityException("User is not authorized to delete this recurring profile.");
        }

        recurringInvoiceRepository.delete(profileToDelete);
    }

    public List<RecurringInvoiceDto> getRecurringInvoicesForUser(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        List<RecurringInvoice> profiles = recurringInvoiceRepository.findAllByShop(user.getShop());
        return profiles.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    private RecurringInvoiceDto mapToDto(RecurringInvoice profile) {
        RecurringInvoiceDto dto = new RecurringInvoiceDto();
        dto.setId(profile.getId());
        dto.setCustomerName(profile.getCustomer().getName());
        dto.setFrequency(profile.getFrequency());
        dto.setStartDate(profile.getStartDate());
        dto.setEndDate(profile.getEndDate());
        dto.setNextIssueDate(profile.getNextIssueDate());
        dto.setAutoSendEmail(profile.isAutoSendEmail());

        List<InvoiceItemDto> itemDtos = profile.getItems().stream().map(item -> {
            InvoiceItemDto itemDto = new InvoiceItemDto();
            itemDto.setProductId(item.getProduct().getId());
            itemDto.setQuantity(item.getQuantity());
            itemDto.setDiscountPercentage(item.getDiscountPercentage());
            return itemDto;
        }).collect(Collectors.toList());
        dto.setItems(itemDtos);

        return dto;
    }
}