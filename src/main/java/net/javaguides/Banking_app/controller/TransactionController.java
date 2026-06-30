package net.javaguides.Banking_app.controller;
// ↑ Belongs to the "controller" package — handles HTTP requests.

import net.javaguides.Banking_app.dto.DashboardResponse;
import net.javaguides.Banking_app.dto.TransactionDto;
import net.javaguides.Banking_app.dto.TransferRequest;
import net.javaguides.Banking_app.service.AccountService;
import net.javaguides.Banking_app.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
// ↑ @DateTimeFormat → tells Spring how to parse a date from a URL query parameter.
//   ISO.DATE_TIME format: "2024-06-19T00:00:00"

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
// ↑ Authentication object contains info about the currently logged-in user.
//   authentication.getName() → returns the user's email (extracted from JWT by JwtAuthenticationFilter).

import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;

// ============================================================
// WHAT IS TransactionController?
// Handles HTTP requests related to money transfers and transaction history.
//
// ENDPOINTS:
//   POST /transfer                        → Send money to another user (by phone number)
//   GET  /transactions/history            → Get your transaction history (paginated, filterable by date)
//   GET  /transactions/{id}               → Get details of one specific transaction
//   GET  /dashboard                       → Get your account dashboard (balance, stats, recent txns)
//
// ALL endpoints require authentication (valid JWT token).
// The logged-in user is automatically identified from the JWT via the Authentication object.
// ============================================================

// @RestController → Handles HTTP requests, returns JSON.
// @RequestMapping (with no path) → No base path prefix; each method defines its own full path.
// @Slf4j → Creates log variable.
@RestController
@RequestMapping
@Slf4j
public class TransactionController {

    private final TransactionService transactionService; // Handles money movement
    private final AccountService accountService;         // Handles account/dashboard data

    // Constructor injection — Spring provides both services automatically.
    public TransactionController(TransactionService transactionService, AccountService accountService) {
        this.transactionService = transactionService;
        this.accountService = accountService;
    }

    // ─── POST /transfer ───────────────────────────────────────────────────
    // Transfer money from the logged-in user to another user (identified by phone number).
    // @Valid → validates TransferRequest fields (amount > 0, phone not blank, etc.)
    // @RequestBody → reads the JSON request body and converts it to a TransferRequest object.
    // Authentication → Spring injects this automatically after JWT validation.
    @PostMapping("/transfer")
    public ResponseEntity<TransactionDto> transfer(
            Authentication authentication,           // Who is making this transfer (from JWT)
            @Valid @RequestBody TransferRequest request) { // Transfer details from request body
        String senderEmail = authentication.getName(); // Get sender's email from JWT token
        log.info("Request to transfer from {} to phone: {}, amount: {}", senderEmail, request.getReceiverPhoneNumber(), request.getAmount());

        // Delegate to TransactionService.transfer() which handles:
        //   - Finding sender by email
        //   - Finding receiver by phone number
        //   - Validating balances and account statuses
        //   - Deducting from sender, adding to receiver
        //   - Saving the transaction record
        TransactionDto transactionDto = transactionService.transfer(
                senderEmail,                          // Sender's email (from JWT)
                request.getReceiverPhoneNumber(),     // Receiver's phone (from request body)
                request.getAmount(),                  // Amount to transfer (from request body)
                request.getRemarks());                // Optional note (from request body)
        return ResponseEntity.ok(transactionDto); // Return transaction receipt with HTTP 200
    }

    // ─── GET /transactions/history ────────────────────────────────────────
    // Get the logged-in user's transaction history.
    // Optional query parameters for date range filtering and pagination.
    //
    // Example URL: GET /transactions/history?start=2024-01-01T00:00:00&end=2024-06-30T23:59:59&page=0&size=20
    @GetMapping("/transactions/history")
    public ResponseEntity<Page<TransactionDto>> getHistory(
            Authentication authentication,
            // @DateTimeFormat → parse ISO date-time string from URL (e.g., "2024-06-19T00:00:00")
            // required = false → start/end are OPTIONAL parameters; they can be omitted.
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(defaultValue = "0") int page,    // Page number (0 = first page)
            @RequestParam(defaultValue = "20") int size) { // Items per page (default: 20)
        String email = authentication.getName();
        log.info("Request for transaction history by {} page={} size={}", email, page, size);

        // Sort by "timestamp" DESCENDING → newest transactions appear first.
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());

        return ResponseEntity.ok(transactionService.getTransactionHistory(email, start, end, pageable));
    }

    // ─── GET /transactions/{id} ───────────────────────────────────────────
    // Get the details of a single transaction by its ID.
    // Example: GET /transactions/42 → returns details of transaction with ID 42.
    // @PathVariable Long id → reads the {id} part from the URL.
    @GetMapping("/transactions/{id}")
    public ResponseEntity<TransactionDto> getTransactionById(@PathVariable Long id) {
        log.info("Fetch transaction details for ID: {}", id);
        return ResponseEntity.ok(transactionService.getTransactionById(id));
    }

    // ─── GET /dashboard ───────────────────────────────────────────────────
    // Get the dashboard data for the currently logged-in user.
    // Returns: account balance, total deposits, total withdrawals, last 5 transactions.
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard(Authentication authentication) {
        String email = authentication.getName();
        log.info("Fetch dashboard for user: {}", email);
        return ResponseEntity.ok(accountService.getUserDashboard(email));
    }
}
