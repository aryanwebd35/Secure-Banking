package net.javaguides.Banking_app.service;
// ↑ Belongs to the "service" package — services contain business logic.

import java.util.List;
// ↑ Standard Java List — used to return multiple AccountDto objects.

import net.javaguides.Banking_app.dto.AccountDto;
// ↑ Imports AccountDto — the data container used to communicate with controllers.

import net.javaguides.Banking_app.dto.DashboardResponse;
// ↑ Imports DashboardResponse — a special DTO containing dashboard statistics.

// ============================================================
// WHAT IS AccountService?
// This is a Java INTERFACE — it defines a CONTRACT.
// An interface says: "WHAT operations are available" but NOT "HOW they work."
//
// WHY USE AN INTERFACE?
// 1. ABSTRACTION: The controller only needs to know WHAT it can call, not HOW.
// 2. FLEXIBILITY: You could swap out AccountServiceImpl with a different implementation
//    (e.g., for testing) without changing the controller at all.
// 3. BEST PRACTICE in Spring Boot: Interface + Implementation is the standard pattern.
//
// THE ACTUAL LOGIC lives in AccountServiceImpl.java
// ============================================================
public interface AccountService {

    // Create a new bank account. Takes AccountDto from request, returns saved AccountDto.
    AccountDto createAccount(AccountDto accountDto);

    // Find and return one account by its numeric ID (primary key).
    AccountDto getAccountById(Long id);

    // Add money (deposit) to a specific account. Returns updated account info.
    AccountDto deposit(Long id, double amount);

    // Remove money (withdraw) from a specific account. Returns updated account info.
    AccountDto withdraw(Long id, double amount);

    // Return a list of ALL accounts in the database (for admin use).
    List<AccountDto> getAllAccounts();

    // Permanently delete an account from the database by its ID.
    void deleteAccount(Long id);

    // Build and return dashboard data for a specific user (identified by email).
    // Dashboard includes: balance, total deposits, total withdrawals, recent transactions.
    DashboardResponse getUserDashboard(String email);

    // Find and return the account belonging to the currently logged-in user (by email).
    AccountDto getMyAccount(String email);
}
