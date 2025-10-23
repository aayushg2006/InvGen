package com.invoice.generator.repository;

import com.invoice.generator.dto.PaymentSummaryDto;
import com.invoice.generator.dto.RevenueByCustomerDto;
import com.invoice.generator.dto.SalesByProductDto;
import com.invoice.generator.model.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReportingRepository extends JpaRepository<Shop, Long> {

    @Query("SELECT new com.invoice.generator.dto.PaymentSummaryDto(COALESCE(p.paymentMethod, 'N/A'), SUM(p.amount)) " +
           "FROM Payment p WHERE p.invoice.shop = :shop AND p.paymentDate BETWEEN :startDate AND :endDate " +
           "GROUP BY p.paymentMethod")
    List<PaymentSummaryDto> getPaymentSummaryByMethod(
        @Param("shop") Shop shop,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT new com.invoice.generator.dto.RevenueByCustomerDto(i.customer.name, SUM(p.amount)) " +
           "FROM Payment p JOIN p.invoice i " +
           "WHERE i.shop = :shop AND p.paymentDate BETWEEN :startDate AND :endDate " +
           "GROUP BY i.customer.name " +
           "ORDER BY SUM(p.amount) DESC")
    List<RevenueByCustomerDto> getRevenueByCustomer(@Param("shop") Shop shop, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT new com.invoice.generator.dto.SalesByProductDto(ii.product.name, SUM(ii.quantity), SUM(ii.pricePerUnit * ii.quantity)) " +
           "FROM InvoiceItem ii JOIN ii.invoice i " +
           "WHERE i.shop = :shop AND i.status = 'PAID' AND i.issueDate BETWEEN :startDate AND :endDate " +
           "GROUP BY ii.product.name " +
           "ORDER BY SUM(ii.pricePerUnit * ii.quantity) DESC")
    List<SalesByProductDto> getSalesByProduct(@Param("shop") Shop shop, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT SUM(p.amount) FROM Payment p JOIN p.invoice i WHERE i.shop = :shop AND p.paymentDate BETWEEN :startDate AND :endDate")
    BigDecimal sumRevenueByShopAndDateRange(@Param("shop") Shop shop, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(ii.quantity * p.costPrice) FROM InvoiceItem ii JOIN ii.invoice i JOIN ii.product p WHERE i.shop = :shop AND i.status = 'PAID' AND i.issueDate BETWEEN :startDate AND :endDate")
    BigDecimal sumCostOfGoodsSoldByShopAndDateRange(@Param("shop") Shop shop, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}