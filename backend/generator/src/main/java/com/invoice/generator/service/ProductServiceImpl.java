package com.invoice.generator.service;

import com.invoice.generator.dto.ProductDto;
import com.invoice.generator.model.Product;
import com.invoice.generator.model.ProductCategory;
import com.invoice.generator.model.User;
import com.invoice.generator.repository.ProductCategoryRepository;
import com.invoice.generator.repository.ProductRepository;
import com.invoice.generator.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductCategoryRepository categoryRepository;

    public List<ProductDto> getProductsByUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<Product> products = productRepository.findAllByShop(user.getShop());
        
        return products.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional
    public Product createProduct(ProductDto productDto, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        ProductCategory category = categoryRepository.findById(productDto.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Product newProduct = new Product();
        newProduct.setName(productDto.getName());
        newProduct.setSellingPrice(productDto.getSellingPrice());
        newProduct.setCostPrice(productDto.getCostPrice());
        newProduct.setQuantityInStock(productDto.getQuantityInStock());
        newProduct.setLowStockThreshold(productDto.getLowStockThreshold());
        newProduct.setCategory(category);
        newProduct.setShop(user.getShop());

        return productRepository.save(newProduct);
    }

    @Transactional
    public ProductDto updateProduct(Long productId, ProductDto productDto, String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        Product productToUpdate = productRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("Product not found with id: " + productId));

        if (!productToUpdate.getShop().getId().equals(user.getShop().getId())) {
            throw new SecurityException("User is not authorized to update this product.");
        }

        ProductCategory category = categoryRepository.findById(productDto.getCategoryId())
            .orElseThrow(() -> new RuntimeException("Category not found"));

        productToUpdate.setName(productDto.getName());
        productToUpdate.setCostPrice(productDto.getCostPrice());
        productToUpdate.setSellingPrice(productDto.getSellingPrice());
        productToUpdate.setQuantityInStock(productDto.getQuantityInStock());
        productToUpdate.setLowStockThreshold(productDto.getLowStockThreshold());
        productToUpdate.setCategory(category);

        Product savedProduct = productRepository.save(productToUpdate);
        return mapToDto(savedProduct);
    }

    public List<ProductDto> getLowStockProducts(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        List<Product> allProducts = productRepository.findAllByShop(user.getShop());
        
        return allProducts.stream()
                .filter(p -> p.getQuantityInStock() != null && 
                             (p.getLowStockThreshold() != null ? p.getQuantityInStock() <= p.getLowStockThreshold() : p.getQuantityInStock() <= 10))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private ProductDto mapToDto(Product product) {
        ProductDto dto = new ProductDto();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setSellingPrice(product.getSellingPrice());
        dto.setCostPrice(product.getCostPrice());
        dto.setQuantityInStock(product.getQuantityInStock());
        dto.setLowStockThreshold(product.getLowStockThreshold());
        dto.setCategoryId(product.getCategory().getId());
        dto.setCategoryName(product.getCategory().getCategoryName());
        dto.setGstPercentage(product.getCategory().getGstPercentage());
        return dto;
    }
}