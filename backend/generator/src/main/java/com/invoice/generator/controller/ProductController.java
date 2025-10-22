package com.invoice.generator.controller;

import com.invoice.generator.dto.ProductDto;
import com.invoice.generator.service.ProductServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Autowired
    private ProductServiceImpl productService;

    @PostMapping
    public ResponseEntity<String> createProduct(@RequestBody ProductDto productDto, @AuthenticationPrincipal UserDetails userDetails) {
        productService.createProduct(productDto, userDetails.getUsername());
        return new ResponseEntity<>("Product created successfully", HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ProductDto>> getCurrentUserProducts(@AuthenticationPrincipal UserDetails userDetails) {
        List<ProductDto> products = productService.getProductsByUser(userDetails.getUsername());
        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDto> updateProduct(@PathVariable Long id, @RequestBody ProductDto productDto, @AuthenticationPrincipal UserDetails userDetails) {
        ProductDto updatedProduct = productService.updateProduct(id, productDto, userDetails.getUsername());
        return new ResponseEntity<>(updatedProduct, HttpStatus.OK);
    }

    @GetMapping("/low-stock")
    public ResponseEntity<List<ProductDto>> getLowStockProducts(@AuthenticationPrincipal UserDetails userDetails) {
        List<ProductDto> lowStockProducts = productService.getLowStockProducts(userDetails.getUsername());
        return ResponseEntity.ok(lowStockProducts);
    }
}