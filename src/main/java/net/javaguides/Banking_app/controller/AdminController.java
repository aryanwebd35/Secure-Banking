package net.javaguides.Banking_app.controller;

import net.javaguides.Banking_app.dto.AccountDto;
import net.javaguides.Banking_app.dto.TransactionDto;
import net.javaguides.Banking_app.dto.UserDto;
import net.javaguides.Banking_app.service.AdminService;
import net.javaguides.Banking_app.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@Slf4j
public class AdminController {

    private final AdminService adminService;
    private final TransactionService transactionService;

    public AdminController(AdminService adminService, TransactionService transactionService) {
        this.adminService = adminService;
        this.transactionService = transactionService;
    }

    @GetMapping("/users")
    public ResponseEntity<Page<UserDto>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort) {
        log.info("Admin fetching all users page={} size={} sort={}", page, size, sort);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        return ResponseEntity.ok(adminService.getAllUsers(pageable));
    }

    @GetMapping("/accounts")
    public ResponseEntity<Page<AccountDto>> getAllAccounts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort) {
        log.info("Admin fetching all accounts page={} size={} sort={}", page, size, sort);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort));
        return ResponseEntity.ok(adminService.getAllAccounts(pageable));
    }

    @GetMapping("/transactions")
    public ResponseEntity<Page<TransactionDto>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "timestamp") String sort) {
        log.info("Admin fetching all transactions page={} size={} sort={}", page, size, sort);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sort).descending());
        return ResponseEntity.ok(transactionService.getAllTransactions(pageable));
    }

    @PutMapping("/block-account/{id}")
    public ResponseEntity<AccountDto> blockAccount(@PathVariable Long id) {
        log.info("Admin blocking account ID: {}", id);
        return ResponseEntity.ok(adminService.updateAccountStatus(id, "BLOCKED"));
    }

    @PutMapping("/unblock-account/{id}")
    public ResponseEntity<AccountDto> unblockAccount(@PathVariable Long id) {
        log.info("Admin unblocking account ID: {}", id);
        return ResponseEntity.ok(adminService.updateAccountStatus(id, "ACTIVE"));
    }

    @PutMapping("/close-account/{id}")
    public ResponseEntity<AccountDto> closeAccount(@PathVariable Long id) {
        log.info("Admin closing account ID: {}", id);
        return ResponseEntity.ok(adminService.updateAccountStatus(id, "CLOSED"));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getAdminDashboard() {
        log.info("Admin fetching dashboard stats");
        return ResponseEntity.ok(adminService.getAdminDashboardStats());
    }
}
