package com.invoice.generator.service;

import com.invoice.generator.dto.ShopSettingsDto;
import com.invoice.generator.model.Shop;
import com.invoice.generator.model.User;
import com.invoice.generator.repository.ShopRepository;
import com.invoice.generator.repository.UserRepository;
import com.razorpay.RazorpayException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class ShopServiceImpl {

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RazorpayService razorpayService;

    public ShopSettingsDto getShopSettingsByUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return mapToDto(user.getShop());
    }

    public ShopSettingsDto updateShopSettings(ShopSettingsDto settingsDto, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Shop shopToUpdate = user.getShop();
        shopToUpdate.setShopName(settingsDto.getShopName());
        shopToUpdate.setGstin(settingsDto.getGstin());
        shopToUpdate.setAddress(settingsDto.getAddress());
        shopToUpdate.setInvoiceAccentColor(settingsDto.getInvoiceAccentColor());
        shopToUpdate.setInvoiceTitle(settingsDto.getInvoiceTitle());
        shopToUpdate.setInvoiceFooter(settingsDto.getInvoiceFooter());

        shopToUpdate.setBeneficiaryName(settingsDto.getBeneficiaryName());
        shopToUpdate.setBankAccountNumber(settingsDto.getBankAccountNumber());
        shopToUpdate.setBankIfscCode(settingsDto.getBankIfscCode());
        // This is the fix: Use getPaymentsEnabled() and a null-safe check
        shopToUpdate.setPaymentsEnabled(Boolean.TRUE.equals(settingsDto.getPaymentsEnabled()));

        // This is the fix: Use a null-safe check for the 'if' condition
        if (Boolean.TRUE.equals(settingsDto.getPaymentsEnabled()) &&
            settingsDto.getBankAccountNumber() != null && !settingsDto.getBankAccountNumber().isEmpty() &&
            settingsDto.getBankIfscCode() != null && !settingsDto.getBankIfscCode().isEmpty()) {

            if (shopToUpdate.getRazorpayFundAccountId() == null || shopToUpdate.getRazorpayFundAccountId().isEmpty()) {
                try {
                    String fundAccountId = razorpayService.createFundAccount(shopToUpdate);
                    shopToUpdate.setRazorpayFundAccountId(fundAccountId);
                } catch (RazorpayException | IOException e) {
                    throw new RuntimeException("Could not create Razorpay fund account: " + e.getMessage(), e);
                }
            }
        }

        Shop updatedShop = shopRepository.save(shopToUpdate);
        return mapToDto(updatedShop);
    }

    private ShopSettingsDto mapToDto(Shop shop) {
        ShopSettingsDto dto = new ShopSettingsDto();
        dto.setShopName(shop.getShopName());
        dto.setGstin(shop.getGstin());
        dto.setAddress(shop.getAddress());
        dto.setLogoPath(shop.getLogoPath());
        dto.setInvoiceAccentColor(shop.getInvoiceAccentColor());
        dto.setInvoiceTitle(shop.getInvoiceTitle());
        dto.setInvoiceFooter(shop.getInvoiceFooter());

        dto.setBeneficiaryName(shop.getBeneficiaryName());
        dto.setBankAccountNumber(shop.getBankAccountNumber());
        dto.setBankIfscCode(shop.getBankIfscCode());
        // This is the fix: Use getPaymentsEnabled()
        dto.setPaymentsEnabled(shop.getPaymentsEnabled());

        return dto;
    }
}