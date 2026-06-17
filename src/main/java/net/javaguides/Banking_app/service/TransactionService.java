package net.javaguides.Banking_app.service;

import net.javaguides.Banking_app.dto.TransactionDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface TransactionService {
    TransactionDto transfer(String senderEmail, String receiverPhoneNumber, double amount, String remarks);
    TransactionDto deposit(Long accountId, double amount);
    TransactionDto withdraw(Long accountId, double amount);
    Page<TransactionDto> getTransactionHistory(String userEmail, LocalDateTime start, LocalDateTime end, Pageable pageable);
    TransactionDto getTransactionById(Long id);
    Page<TransactionDto> getAllTransactions(Pageable pageable);
}
