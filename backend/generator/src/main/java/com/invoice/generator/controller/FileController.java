package com.invoice.generator.controller;

import com.invoice.generator.model.Shop;
import com.invoice.generator.model.User;
import com.invoice.generator.repository.ShopRepository;
import com.invoice.generator.repository.UserRepository;
import com.invoice.generator.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Autowired
    private FileStorageService fileStorageService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ShopRepository shopRepository;

    @PostMapping("/uploadLogo")
    @Transactional
    public ResponseEntity<String> uploadLogo(@RequestParam("logo") MultipartFile file, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        Long shopId = user.getShop().getId();
        Shop shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new RuntimeException("Shop not found for the current user"));

        String originalFileName = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFileName != null && originalFileName.contains(".")) {
            fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
        }
        String newFileName = "shop-" + shop.getId() + "-logo" + fileExtension;
        
        fileStorageService.storeFile(file, newFileName);
        
        shop.setLogoPath("/logos/" + newFileName);
        shopRepository.save(shop);

        return ResponseEntity.ok("Logo uploaded and path saved successfully: " + newFileName);
    }
}