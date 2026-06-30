package net.javaguides.Banking_app.service;
// ↑ Belongs to the "service" package.

import net.javaguides.Banking_app.dto.TransactionDto;
// ↑ Imports TransactionDto — the data container for transaction information.

import org.springframework.data.domain.Page;
// ↑ Page<T> = paginated results (one "page" of records, not all at once).

import org.springframework.data.domain.Pageable;
// ↑ Pageable = instructions for pagination (page number, page size, sort direction).

import java.time.LocalDateTime;
// ↑ Java's date-time class. Used for filtering transactions by date range.

// ============================================================
// WHAT IS TransactionService?
// An interface that defines WHAT transaction operations are available.
// The actual code lives in TransactionServiceImpl.java.
//
// IMPORTANT: TransactionService is the heart of this banking app.
// Money NEVER moves without going through this service.
// Even AccountServiceImpl calls this service to do deposits and withdrawals.
// ============================================================
public interface TransactionService {

    // Transfer money from one user (identified by email) to another (identified by phone number).
    // Returns a TransactionDto describing the completed transfer.
    TransactionDto transfer(String senderEmail, String receiverPhoneNumber, double amount, String remarks);

    // Add money to an account (deposit). Returns transaction record.
    TransactionDto deposit(Long accountId, double amount);

    // Remove money from an account (withdrawal). Returns transaction record.
    TransactionDto withdraw(Long accountId, double amount);

    // Get paginated transaction history for a user (filtered by optional date range).
    // If start/end are null, returns ALL transactions (paginated).
    Page<TransactionDto> getTransactionHistory(String userEmail, LocalDateTime start, LocalDateTime end, Pageable pageable);

    // Get a single transaction by its ID.
    TransactionDto getTransactionById(Long id);

    // Admin only: Get ALL transactions in the system (paginated), for the admin dashboard.
    Page<TransactionDto> getAllTransactions(Pageable pageable);
}
