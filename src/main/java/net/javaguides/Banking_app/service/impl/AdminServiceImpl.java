package net.javaguides.Banking_app.service.impl;

import net.javaguides.Banking_app.dto.AccountDto;
import net.javaguides.Banking_app.dto.UserDto;
import net.javaguides.Banking_app.entity.Account;
import net.javaguides.Banking_app.entity.AccountStatus;
import net.javaguides.Banking_app.entity.User;
import net.javaguides.Banking_app.exception.AccountNotFoundException;
import net.javaguides.Banking_app.mapper.AccountMapper;
import net.javaguides.Banking_app.mapper.UserMapper;
import net.javaguides.Banking_app.repository.AccountRepository;
import net.javaguides.Banking_app.repository.TransactionRepository;
import net.javaguides.Banking_app.repository.UserRepository;
import net.javaguides.Banking_app.service.AdminService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// ============================================================
// WHAT IS AdminServiceImpl?
// The implementation of AdminService. Contains all admin-only operations.
//
// WHO CAN ACCESS THIS?
// Only users with "ROLE_ADMIN" — enforced in SecurityConfig.java:
//   .requestMatchers("/admin/**").hasRole("ADMIN")
// The admin logs in via /api/auth/admin/login with email + password.
// Credentials are stored in application.properties (not in the database).
//
// ADMIN DASHBOARD DATA:
//   - Total money in the entire banking system
//   - Top 5 wealthiest account holders
//   - Total transactions count
//   - Total users and accounts count
//   - Can block, unblock, or close any account
// ============================================================

// @Service → Spring registers this as a service bean.
// @Slf4j → Auto-creates a log variable for structured logging.
@Service
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;           // Access to "users" table
    private final AccountRepository accountRepository;     // Access to "accounts" table
    private final TransactionRepository transactionRepository; // Access to "transactions" table

    // Constructor injection — Spring provides all three repositories automatically.
    public AdminServiceImpl(UserRepository userRepository, AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    // ============================================================
    // GET ALL USERS (paginated)
    // ============================================================
    @Override
    public Page<UserDto> getAllUsers(Pageable pageable) {
        // userRepository.findAll(pageable):
        //   → Runs: SELECT * FROM users LIMIT ? OFFSET ? ORDER BY ?
        //   → Returns: Page<User> — one page of User entities
        // .map(UserMapper::mapToUserDto):
        //   → Converts each User entity in the page → UserDto
        return userRepository.findAll(pageable)
                .map(UserMapper::mapToUserDto);
    }

    // ============================================================
    // GET ALL ACCOUNTS (paginated)
    // ============================================================
    @Override
    public Page<AccountDto> getAllAccounts(Pageable pageable) {
        // Same pattern — get paginated Account entities, map each to AccountDto.
        return accountRepository.findAll(pageable)
                .map(AccountMapper::mapToAccountDto);
    }

    // ============================================================
    // UPDATE ACCOUNT STATUS (block/unblock/close an account)
    // ============================================================
    @Override
    @Transactional
    // ↑ @Transactional: if saving fails, the change is rolled back (account status unchanged).
    public AccountDto updateAccountStatus(Long accountId, String status) {
        log.info("Admin updating account ID {} status to {}", accountId, status);

        // Step 1: Find the account by ID.
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for ID: " + accountId));

        // Step 2: Convert the incoming status String → AccountStatus enum.
        // status.toUpperCase() → ensures "blocked", "BLOCKED", "Blocked" all work.
        // AccountStatus.valueOf("BLOCKED") → converts String to the AccountStatus.BLOCKED enum value.
        // If an invalid string is provided, valueOf() throws an exception automatically.
        AccountStatus targetStatus = AccountStatus.valueOf(status.toUpperCase());

        // Step 3: Update the account status.
        account.setAccountStatus(targetStatus);

        // Step 4: Save the updated account back to the database.
        Account saved = accountRepository.save(account);

        // Step 5: Convert Entity → DTO and return the updated account info.
        return AccountMapper.mapToAccountDto(saved);
    }

    // ============================================================
    // ADMIN DASHBOARD STATISTICS
    // ============================================================
    @Override
    public Map<String, Object> getAdminDashboardStats() {
        log.info("Fetching Admin Dashboard Statistics");

        // A Map<String, Object> holds key-value pairs.
        // Key = String (name of the stat), Value = Object (the actual value, can be any type).
        // Example: {"totalMoneyInSystem": 150000.0, "totalUsersCount": 42}
        Map<String, Object> stats = new HashMap<>();

        // ── Stat 1: Total money in the system ──────────────────
        // Calls the custom @Query in AccountRepository:
        //   SELECT COALESCE(SUM(a.balance), 0.0) FROM Account a
        // Returns the SUM of all balances across all accounts.
        Double totalMoney = accountRepository.sumAllBalances();
        stats.put("totalMoneyInSystem", totalMoney);

        // ── Stat 2: Top 5 account holders by balance ───────────
        // PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "balance")):
        //   → Page 0 (first page), size 5 (only 5 results)
        //   → Sorted by "balance" column in DESCENDING order (highest balance first)
        Pageable topFive = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "balance"));

        // findAll(topFive) → SELECT * FROM accounts ORDER BY balance DESC LIMIT 5
        // .getContent() → extracts the List<Account> from the Page object
        // .stream().map(...).collect(...) → converts each Account → AccountDto
        List<AccountDto> topAccountHolders = accountRepository.findAll(topFive)
                .getContent()
                .stream()
                .map(AccountMapper::mapToAccountDto)
                .collect(Collectors.toList());
        stats.put("topAccountHolders", topAccountHolders);

        // ── Stat 3: Total number of transactions ever ──────────
        // .count() from JpaRepository → SELECT COUNT(*) FROM transactions
        long totalTransactions = transactionRepository.count();
        stats.put("totalTransactionsCount", totalTransactions);

        // ── Stat 4 & 5: Total users and accounts ───────────────
        stats.put("totalUsersCount", userRepository.count());    // SELECT COUNT(*) FROM users
        stats.put("totalAccountsCount", accountRepository.count()); // SELECT COUNT(*) FROM accounts

        return stats; // Return all statistics as a Map (will be serialized to JSON by Spring).
    }
}
