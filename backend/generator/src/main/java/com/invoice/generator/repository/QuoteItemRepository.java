package com.invoice.generator.repository;

import com.invoice.generator.model.Quote;
import com.invoice.generator.model.QuoteItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuoteItemRepository extends JpaRepository<QuoteItem, Long> {
    List<QuoteItem> findAllByQuote(Quote quote);
}