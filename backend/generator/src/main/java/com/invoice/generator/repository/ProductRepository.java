package com.invoice.generator.repository;

import com.invoice.generator.model.Product;
import com.invoice.generator.model.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Add this method to find all products associated with a shop
    List<Product> findAllByShop(Shop shop);
}