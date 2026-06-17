package net.javaguides.Banking_app.controller;

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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;

@RestController
@RequestMapping
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;
    private final AccountService accountService;

    public TransactionController(TransactionService transactionService, AccountService accountService) {
        this.transactionService = transactionService;
        this.accountService = accountService;
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionDto> transfer(
            Authentication authentication,
            @Valid @RequestBody TransferRequest request) {
        String senderEmail = authentication.getName();
        log.info("Request to transfer from {} to phone: {}, amount: {}", senderEmail, request.getReceiverPhoneNumber(), request.getAmount());
        TransactionDto transactionDto = transactionService.transfer(
                senderEmail, request.getReceiverPhoneNumber(), request.getAmount(), request.getRemarks());
        return ResponseEntity.ok(transactionDto);
    }

    @GetMapping("/transactions/history")
    public ResponseEntity<Page<TransactionDto>> getHistory(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String email = authentication.getName();
        log.info("Request for transaction history by {} page={} size={}", email, page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return ResponseEntity.ok(transactionService.getTransactionHistory(email, start, end, pageable));
    }

    @GetMapping("/transactions/{id}")
    public ResponseEntity<TransactionDto> getTransactionById(@PathVariable Long id) {
        log.info("Fetch transaction details for ID: {}", id);
        return ResponseEntity.ok(transactionService.getTransactionById(id));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboard(Authentication authentication) {
        String email = authentication.getName();
        log.info("Fetch dashboard for user: {}", email);
        return ResponseEntity.ok(accountService.getUserDashboard(email));
    }
}
