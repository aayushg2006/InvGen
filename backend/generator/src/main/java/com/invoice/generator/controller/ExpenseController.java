package com.invoice.generator.controller;

import com.invoice.generator.dto.ExpenseDto;
import com.invoice.generator.service.ExpenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    @Autowired
    private ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<?> createExpense(@RequestBody ExpenseDto expenseDto, @AuthenticationPrincipal UserDetails userDetails) {
        expenseService.createExpense(expenseDto, userDetails.getUsername());
        return new ResponseEntity<>("Expense created successfully", HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<ExpenseDto>> getExpenses(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(expenseService.getExpensesForUser(userDetails.getUsername()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpenseDto> updateExpense(@PathVariable Long id, @RequestBody ExpenseDto expenseDto, @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(expenseService.updateExpense(id, expenseDto, userDetails.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteExpense(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        expenseService.deleteExpense(id, userDetails.getUsername());
        return ResponseEntity.ok("Expense deleted successfully");
    }
}