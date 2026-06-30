package net.javaguides.Banking_app.service.impl;
// ↑ This file is in the "impl" sub-package. "impl" is short for "implementation."
//   Service interfaces define WHAT to do. This class defines HOW.

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
// ↑ stream() and collect() are Java Stream API tools used for processing collections.
//   Think of a stream as a pipeline: [list of items] → filter → transform → collect into new list.

import org.springframework.context.annotation.Lazy;
// ↑ @Lazy tells Spring: "Create this dependency ONLY when it is first needed,
//   not at application startup." Used here to break a circular dependency.

import org.springframework.stereotype.Service;
// ↑ @Service marks this class as a Spring "service bean."
//   Spring will automatically create one instance of this class and manage it.
//   It can then be "injected" into Controllers using dependency injection.

import net.javaguides.Banking_app.dto.AccountDto;
import net.javaguides.Banking_app.dto.DashboardResponse;
import net.javaguides.Banking_app.dto.TransactionDto;
import net.javaguides.Banking_app.entity.Account;
import net.javaguides.Banking_app.entity.Transaction;
import net.javaguides.Banking_app.entity.TransactionType;
import net.javaguides.Banking_app.entity.TransactionStatus;
import net.javaguides.Banking_app.exception.AccountNotFoundException;
import net.javaguides.Banking_app.mapper.AccountMapper;
import net.javaguides.Banking_app.mapper.TransactionMapper;
import net.javaguides.Banking_app.repository.AccountRepository;
import net.javaguides.Banking_app.repository.TransactionRepository;
import net.javaguides.Banking_app.service.AccountService;
import net.javaguides.Banking_app.service.TransactionService;
// ↑ All imports for entities, DTOs, exceptions, mappers, repositories, and service interfaces.

// ============================================================
// WHAT IS AccountServiceImpl?
// This is the IMPLEMENTATION of the AccountService interface.
// It contains the actual business logic for account operations.
//
// WHERE IT FITS IN THE ARCHITECTURE:
//   HTTP Request → Controller → AccountServiceImpl → AccountRepository → MySQL
//                                                  ↘ TransactionService (for deposits/withdrawals)
//
// KEY CONCEPT - DEPENDENCY INJECTION:
// Instead of creating objects like: AccountRepository repo = new AccountRepository();
// Spring automatically "injects" (provides) the objects via the constructor.
// ============================================================

// @Service → Tells Spring: "Register this class as a Service bean.
//   When something needs AccountService, provide this implementation."
@Service
public class AccountServiceImpl implements AccountService {

    // These are the "dependencies" — other services/repositories this class needs.
    // They are declared as "final" — they are set once in the constructor and never changed.
    private final AccountRepository accountRepository;         // Talks to the "accounts" table
    private final TransactionRepository transactionRepository; // Talks to the "transactions" table
    private final TransactionService transactionService;       // Handles the money movement logic

    // CONSTRUCTOR INJECTION:
    // Spring sees this constructor and knows: "I need to provide AccountRepository,
    // TransactionRepository, and TransactionService when creating AccountServiceImpl."
    // Spring automatically finds the implementations and passes them in.
    //
    // WHY @Lazy on TransactionService?
    // AccountService uses TransactionService, but TransactionServiceImpl uses AccountRepository.
    // This creates a circular dependency: A needs B, B needs A (Spring gets confused on startup).
    // @Lazy tells Spring: "Don't inject TransactionService NOW at startup.
    //   Inject it later, only when a method that needs it is actually called."
    public AccountServiceImpl(AccountRepository accountRepository,
                              TransactionRepository transactionRepository,
                              @Lazy TransactionService transactionService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.transactionService = transactionService;
    }

    // @Override → We are "implementing" this method from the AccountService interface.
    @Override
    public AccountDto createAccount(AccountDto accountDto) {
        // Step 1: Convert the incoming DTO (from HTTP request) → Entity (for database)
        Account account = AccountMapper.mapToAccount(accountDto);

        // Step 2: Save the entity to the database. Returns the saved entity with the generated ID.
        Account savedAccount = accountRepository.save(account);

        // Step 3: Convert the saved Entity back → DTO (to send in HTTP response)
        return AccountMapper.mapToAccountDto(savedAccount);
    }

    @Override
    public AccountDto getAccountById(Long id) {
        // findById(id) returns an Optional<Account>
        // .orElseThrow() → if the Optional is empty (not found), throw an exception.
        // AccountNotFoundException extends RuntimeException — it's caught by GlobalExceptionHandler.
        Account account = accountRepository
                .findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for ID: " + id));

        // Convert Entity → DTO and return it
        return AccountMapper.mapToAccountDto(account);
    }

    @Override
    public AccountDto deposit(Long id, double amount) {
        // Delegate the actual deposit operation to TransactionService.
        // TransactionService handles: validation, balance update, and saving the transaction record.
        transactionService.deposit(id, amount);

        // After deposit, re-fetch the account from DB to get the UPDATED balance.
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for ID: " + id));

        // Return the updated account info as a DTO.
        return AccountMapper.mapToAccountDto(account);
    }

    @Override
    public AccountDto withdraw(Long id, double amount) {
        // Same pattern as deposit — delegate to TransactionService.
        transactionService.withdraw(id, amount);

        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for ID: " + id));

        return AccountMapper.mapToAccountDto(account);
    }

    @Override
    public List<AccountDto> getAllAccounts() {
        // Get all Account entities from the database.
        List<Account> accounts = accountRepository.findAll();

        // Convert each Account entity to an AccountDto using the stream API:
        //   .stream()                  → create a processing pipeline from the list
        //   .map(AccountMapper::mapToAccountDto)  → transform each Account → AccountDto
        //   .collect(Collectors.toList())  → collect results into a new List
        return accounts.stream()
                .map(AccountMapper::mapToAccountDto)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteAccount(Long id) {
        // First, verify the account exists. If not, throw a clear error.
        Account account = accountRepository
                .findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for ID: " + id));

        // If found, delete it from the database permanently.
        accountRepository.delete(account);
    }

    @Override
    public DashboardResponse getUserDashboard(String email) {
        // Step 1: Find the account for this email.
        Account account = accountRepository.findByUserEmail(email)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for email: " + email));

        // Step 2: Get ALL transactions for this account (as sender OR receiver).
        List<Transaction> allTransactions = transactionRepository.findAllByAccountId(account.getId());

        // Step 3: Calculate TOTAL DEPOSITS (successful incoming money)
        // Stream pipeline:
        //   filter(SUCCESS status only)
        //   filter(type is DEPOSIT, OR it's a TRANSFER where THIS account is the RECEIVER)
        //   .mapToDouble(Transaction::getAmount) → extract just the amount numbers
        //   .sum() → add them all up
        double totalDeposits = allTransactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)
                .filter(t -> t.getTransactionType() == TransactionType.DEPOSIT ||
                        (t.getTransactionType() == TransactionType.TRANSFER && account.getId().equals(t.getReceiverAccountId())))
                .mapToDouble(Transaction::getAmount)
                .sum();

        // Step 4: Calculate TOTAL WITHDRAWALS (successful outgoing money)
        double totalWithdrawals = allTransactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)
                .filter(t -> t.getTransactionType() == TransactionType.WITHDRAW ||
                        (t.getTransactionType() == TransactionType.TRANSFER && account.getId().equals(t.getSenderAccountId())))
                .mapToDouble(Transaction::getAmount)
                .sum();

        // Step 5: Get the 5 most recent transactions for the "recent activity" section.
        // .sorted() → sort by timestamp, newest first (t2 compared to t1 = descending order)
        // .limit(5) → take only the first 5 results
        // .map() → convert each Transaction entity → TransactionDto
        List<TransactionDto> recentTransactions = allTransactions.stream()
                .sorted((t1, t2) -> t2.getTimestamp().compareTo(t1.getTimestamp()))
                .limit(5)
                .map(TransactionMapper::mapToTransactionDto)
                .collect(Collectors.toList());

        // Step 6: Build and populate the DashboardResponse object with all the data.
        DashboardResponse response = new DashboardResponse();
        response.setAccountBalance(account.getBalance());          // Current balance
        response.setTotalDeposits(totalDeposits);                  // Sum of all deposits
        response.setTotalWithdrawals(totalWithdrawals);            // Sum of all withdrawals
        response.setRecentTransactions(recentTransactions);        // Last 5 transactions
        response.setAccountId(account.getId());                    // Account ID number
        response.setAccountHolderName(account.getAccountHolderName()); // User's name
        response.setPhoneNumber(account.getUser() != null ? account.getUser().getPhoneNumber() : null); // Phone
        response.setAccountStatus(account.getAccountStatus() != null ? account.getAccountStatus().name() : "ACTIVE"); // Status

        return response;
    }

    @Override
    public AccountDto getMyAccount(String email) {
        // Find account by the logged-in user's email.
        Account account = accountRepository.findByUserEmail(email)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for user: " + email));

        // Convert Entity → DTO and return.
        return AccountMapper.mapToAccountDto(account);
    }
}
