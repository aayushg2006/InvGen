package com.invoice.generator.repository;

import com.invoice.generator.dto.PaymentSummaryDto;
import com.invoice.generator.model.Shop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReportingRepository extends JpaRepository<Shop, Long> {

    // --- THIS QUERY IS NOW MORE ROBUST ---
    @Query("SELECT new com.invoice.generator.dto.PaymentSummaryDto(COALESCE(p.paymentMethod, 'N/A'), SUM(p.amount)) " +
           "FROM Payment p WHERE p.invoice.shop = :shop AND p.paymentDate BETWEEN :startDate AND :endDate " +
           "GROUP BY p.paymentMethod")
    List<PaymentSummaryDto> getPaymentSummaryByMethod(
        @Param("shop") Shop shop,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}