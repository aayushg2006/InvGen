package com.invoice.generator.controller;

import com.invoice.generator.dto.DashboardStatsDto;
import com.invoice.generator.service.DashboardServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardServiceImpl dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsDto> getStats(@AuthenticationPrincipal UserDetails userDetails) {
        DashboardStatsDto stats = dashboardService.getDashboardStats(userDetails.getUsername());
        return ResponseEntity.ok(stats);
    }
}