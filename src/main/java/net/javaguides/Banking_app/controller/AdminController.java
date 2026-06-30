package net.javaguides.Banking_app.controller;
// ↑ Belongs to the "controller" package — controllers handle incoming HTTP requests.

import net.javaguides.Banking_app.dto.AccountDto;
import net.javaguides.Banking_app.dto.TransactionDto;
import net.javaguides.Banking_app.dto.UserDto;
import net.javaguides.Banking_app.service.AdminService;
import net.javaguides.Banking_app.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
// ↑ Page<T> = paginated results — a single "page" of records.

import org.springframework.data.domain.PageRequest;
// ↑ PageRequest.of(page, size, sort) creates a Pageable object specifying: page number, items per page, sort order.

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
// ↑ Sort defines the ordering of results (ascending or descending by a field).

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
// ↑ @RestController, @RequestMapping, @GetMapping, @PutMapping, @RequestParam, @PathVariable

import java.util.Map;

// ============================================================
// WHAT IS AdminController?
// Handles all HTTP requests to /admin/** endpoints.
// ALL methods here require ROLE_ADMIN (enforced by SecurityConfig).
// Regular users CANNOT access these — they'll get a 403 Forbidden error.
//
// ADMIN CAPABILITIES:
//   GET  /admin/users         → List all users (paginated)
//   GET  /admin/accounts      → List all accounts (paginated)
//   GET  /admin/transactions  → List all transactions (paginated)
//   PUT  /admin/block-account/{id}   → Block an account
//   PUT  /admin/unblock-account/{id} → Unblock an account
//   PUT  /admin/close-account/{id}   → Close an account permanently
//   GET  /admin/dashboard     → Get banking system statistics
//
// PAGINATION:
//   ?page=0&size=10&sort=id
//   page: which page (0 = first)
//   size: items per page
//   sort: field to sort by
// ============================================================

// @RestController → This class handles HTTP requests and returns JSON responses.
// @RequestMapping("/admin") → ALL endpoints in this class start with "/admin".
// @Slf4j → Creates a log variable for logging messages to the console.
@RestController
@RequestMapping("/admin")
@Slf4j
public class AdminController {

    private final AdminService adminService;         // Business logic for admin operations
    private final TransactionService transactionService; // Used to get transaction data

    // Constructor injection — Spring automatically provides these services.
    public AdminController(AdminService adminService, TransactionService transactionService) {
        this.adminService = adminService;
        this.transactionService = transactionService;
    }

    // ─── GET /admin/users?page=0&size=10&sort=id ─────────────────────────
    // Returns a paginated list of all registered users.
    // @RequestParam → reads URL query parameters like ?page=0&size=10&sort=id
    // defaultValue → uses these values if the parameter is not provided in the URL.
    @GetMapping("/users")
    public ResponseEntity<Page<UserDto>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,    // Which page (0-indexed). Default: 0
            @RequestParam(defaultValue = "10") int size,   // Items per page. Default: 10
            @RequestParam(defaultValue = "id") String sort) { // Sort by field name. Default: "id"
        log.info("Admin fetching all users page={} size={} sort={}", page, size, sort);

        // Build a Pageable object with page number, size, and sort order.
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));

        // Call service and return result with HTTP 200 OK.
        return ResponseEntity.ok(adminService.getAllUsers(pageable));
    }

    // ─── GET /admin/accounts?page=0&size=10&sort=id ──────────────────────
    // Returns a paginated list of all bank accounts.
    @GetMapping("/accounts")
    public ResponseEntity<Page<AccountDto>> getAllAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort) {
        log.info("Admin fetching all accounts page={} size={} sort={}", page, size, sort);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        return ResponseEntity.ok(adminService.getAllAccounts(pageable));
    }

    // ─── GET /admin/transactions?page=0&size=10&sort=timestamp ───────────
    // Returns a paginated list of ALL transactions in the system.
    @GetMapping("/transactions")
    public ResponseEntity<Page<TransactionDto>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "timestamp") String sort) { // Default sort by timestamp
        log.info("Admin fetching all transactions page={} size={} sort={}", page, size, sort);
        // Sort.by(sort).descending() → newest transactions first.
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort).descending());
        return ResponseEntity.ok(transactionService.getAllTransactions(pageable));
    }

    // ─── PUT /admin/block-account/{id} ───────────────────────────────────
    // Blocks an account — prevents any transactions on it.
    // @PathVariable Long id → reads the {id} from the URL (e.g., /admin/block-account/5)
    @PutMapping("/block-account/{id}")
    public ResponseEntity<AccountDto> blockAccount(@PathVariable Long id) {
        log.info("Admin blocking account ID: {}", id);
        // Calls updateAccountStatus("BLOCKED") in AdminService.
        return ResponseEntity.ok(adminService.updateAccountStatus(id, "BLOCKED"));
    }

    // ─── PUT /admin/unblock-account/{id} ─────────────────────────────────
    // Restores a blocked account back to ACTIVE status.
    @PutMapping("/unblock-account/{id}")
    public ResponseEntity<AccountDto> unblockAccount(@PathVariable Long id) {
        log.info("Admin unblocking account ID: {}", id);
        return ResponseEntity.ok(adminService.updateAccountStatus(id, "ACTIVE"));
    }

    // ─── PUT /admin/close-account/{id} ───────────────────────────────────
    // Permanently closes an account (no transactions ever again).
    @PutMapping("/close-account/{id}")
    public ResponseEntity<AccountDto> closeAccount(@PathVariable Long id) {
        log.info("Admin closing account ID: {}", id);
        return ResponseEntity.ok(adminService.updateAccountStatus(id, "CLOSED"));
    }

    // ─── GET /admin/dashboard ─────────────────────────────────────────────
    // Returns overall system statistics as a Map<String, Object> (JSON key-value object).
    // Contains: total money, top accounts, user count, transaction count, account count.
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getAdminDashboard() {
        log.info("Admin fetching dashboard stats");
        return ResponseEntity.ok(adminService.getAdminDashboardStats());
    }
}
