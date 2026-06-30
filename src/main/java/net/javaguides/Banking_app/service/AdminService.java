package net.javaguides.Banking_app.service;
// ↑ Belongs to the "service" package.

import net.javaguides.Banking_app.dto.AccountDto;
import net.javaguides.Banking_app.dto.UserDto;
// ↑ Imports DTOs — data transfer objects used to communicate between layers.

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
// ↑ Page<T> and Pageable for paginated results (admin lists can be very large).

import java.util.Map;
// ↑ Map<String, Object> used for the dashboard stats (key-value pairs of different types).

// ============================================================
// WHAT IS AdminService?
// An interface defining what the Admin can do in this application.
// The actual code lives in AdminServiceImpl.java.
//
// Admin-only features:
//  → View all users (paginated)
//  → View all accounts (paginated)
//  → Block / Unblock / Close an account
//  → View admin dashboard statistics
// ============================================================
public interface AdminService {

    // Get a paginated list of all users in the system.
    // Page<UserDto> → returns one "page" of user data at a time (e.g., 10 users per page).
    Page<UserDto> getAllUsers(Pageable pageable);

    // Get a paginated list of all bank accounts in the system.
    Page<AccountDto> getAllAccounts(Pageable pageable);

    // Change the status of an account to ACTIVE, BLOCKED, or CLOSED.
    // status parameter is a String like "BLOCKED", "ACTIVE", "CLOSED".
    // Returns the updated AccountDto.
    AccountDto updateAccountStatus(Long accountId, String status);

    // Return a Map (key-value pairs) of admin dashboard statistics:
    //   "totalMoneyInSystem" → total balance across all accounts
    //   "topAccountHolders" → list of top 5 richest accounts
    //   "totalTransactionsCount" → total number of transactions ever
    //   "totalUsersCount" → total number of registered users
    //   "totalAccountsCount" → total number of bank accounts
    Map<String, Object> getAdminDashboardStats();
}
