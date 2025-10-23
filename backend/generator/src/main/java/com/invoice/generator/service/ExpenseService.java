package com.invoice.generator.service;

import com.invoice.generator.dto.ExpenseDto;
import com.invoice.generator.model.Expense;
import com.invoice.generator.model.User;
import com.invoice.generator.repository.ExpenseRepository;
import com.invoice.generator.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public Expense createExpense(ExpenseDto expenseDto, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        Expense expense = new Expense();
        expense.setDescription(expenseDto.getDescription());
        expense.setAmount(expenseDto.getAmount());
        expense.setDate(expenseDto.getDate());
        expense.setCategory(expenseDto.getCategory());
        expense.setShop(user.getShop());

        return expenseRepository.save(expense);
    }

    public List<ExpenseDto> getExpensesForUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return expenseRepository.findAllByShopOrderByDateDesc(user.getShop())
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional
    public ExpenseDto updateExpense(Long expenseId, ExpenseDto expenseDto, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Expense expenseToUpdate = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        if (!expenseToUpdate.getShop().getId().equals(user.getShop().getId())) {
            throw new SecurityException("User not authorized to update this expense.");
        }

        expenseToUpdate.setDescription(expenseDto.getDescription());
        expenseToUpdate.setAmount(expenseDto.getAmount());
        expenseToUpdate.setDate(expenseDto.getDate());
        expenseToUpdate.setCategory(expenseDto.getCategory());

        return mapToDto(expenseRepository.save(expenseToUpdate));
    }

    @Transactional
    public void deleteExpense(Long expenseId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Expense expenseToDelete = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));

        if (!expenseToDelete.getShop().getId().equals(user.getShop().getId())) {
            throw new SecurityException("User not authorized to delete this expense.");
        }
        expenseRepository.deleteById(expenseId);
    }

    private ExpenseDto mapToDto(Expense expense) {
        ExpenseDto dto = new ExpenseDto();
        dto.setId(expense.getId());
        dto.setDescription(expense.getDescription());
        dto.setAmount(expense.getAmount());
        dto.setDate(expense.getDate());
        dto.setCategory(expense.getCategory());
        return dto;
    }
}