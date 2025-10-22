package com.invoice.generator.controller;

import com.invoice.generator.dto.ShopSettingsDto;
import com.invoice.generator.model.Shop;
import com.invoice.generator.service.ShopServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    @Autowired
    private ShopServiceImpl shopService;

    @GetMapping
    public ResponseEntity<ShopSettingsDto> getCurrentShopSettings(@AuthenticationPrincipal UserDetails userDetails) {
        ShopSettingsDto settings = shopService.getShopSettingsByUser(userDetails.getUsername());
        return new ResponseEntity<>(settings, HttpStatus.OK);
    }

    @PutMapping
    public ResponseEntity<ShopSettingsDto> updateShopSettings(@RequestBody ShopSettingsDto settingsDto, @AuthenticationPrincipal UserDetails userDetails) { // Return DTO
        ShopSettingsDto updatedShop = shopService.updateShopSettings(settingsDto, userDetails.getUsername()); // Receive DTO
        return new ResponseEntity<>(updatedShop, HttpStatus.OK);
    }
}