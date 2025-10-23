package com.invoice.generator.repository;

import com.invoice.generator.model.Expense;
import com.invoice.generator.model.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findAllByShopOrderByDateDesc(Shop shop);

    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.shop = :shop AND e.date BETWEEN :startDate AND :endDate")
    BigDecimal sumExpensesByShopAndDateRange(@Param("shop") Shop shop, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}