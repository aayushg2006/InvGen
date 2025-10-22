package com.invoice.generator.repository;

import com.invoice.generator.model.Quote;
import com.invoice.generator.model.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuoteRepository extends JpaRepository<Quote, Long> {
    List<Quote> findAllByShopOrderByIssueDateDesc(Shop shop);
}