package net.javaguides.Banking_app.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

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

@Service
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;

    // Use @Lazy on TransactionService to prevent circular dependency since they refer to each other
    public AccountServiceImpl(AccountRepository accountRepository,
                              TransactionRepository transactionRepository,
                              @Lazy TransactionService transactionService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.transactionService = transactionService;
    }

    @Override
    public AccountDto createAccount(AccountDto accountDto) {
        Account account = AccountMapper.mapToAccount(accountDto);
        Account savedAccount = accountRepository.save(account);
        return AccountMapper.mapToAccountDto(savedAccount);
    }

    @Override
    public AccountDto getAccountById(Long id) {
        Account account = accountRepository
                .findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for ID: " + id));
        return AccountMapper.mapToAccountDto(account);
    }

    @Override
    public AccountDto deposit(Long id, double amount) {
        transactionService.deposit(id, amount);
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for ID: " + id));
        return AccountMapper.mapToAccountDto(account);
    }

    @Override
    public AccountDto withdraw(Long id, double amount) {
        transactionService.withdraw(id, amount);
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for ID: " + id));
        return AccountMapper.mapToAccountDto(account);
    }

    @Override
    public List<AccountDto> getAllAccounts() {
        List<Account> accounts = accountRepository.findAll();
        return accounts.stream()
                .map(AccountMapper::mapToAccountDto)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteAccount(Long id) {
        Account account = accountRepository
                .findById(id)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for ID: " + id));
        accountRepository.delete(account);
    }

    @Override
    public DashboardResponse getUserDashboard(String email) {
        Account account = accountRepository.findByUserEmail(email)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for email: " + email));

        List<Transaction> allTransactions = transactionRepository.findAllByAccountId(account.getId());

        // Calculate total deposits (only successful deposits and successful incoming transfers)
        double totalDeposits = allTransactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)
                .filter(t -> t.getTransactionType() == TransactionType.DEPOSIT ||
                        (t.getTransactionType() == TransactionType.TRANSFER && account.getId().equals(t.getReceiverAccountId())))
                .mapToDouble(Transaction::getAmount)
                .sum();

        // Calculate total withdrawals (only successful withdrawals and successful outgoing transfers)
        double totalWithdrawals = allTransactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.SUCCESS)
                .filter(t -> t.getTransactionType() == TransactionType.WITHDRAW ||
                        (t.getTransactionType() == TransactionType.TRANSFER && account.getId().equals(t.getSenderAccountId())))
                .mapToDouble(Transaction::getAmount)
                .sum();

        // Get recent 5 transactions
        List<TransactionDto> recentTransactions = allTransactions.stream()
                .sorted((t1, t2) -> t2.getTimestamp().compareTo(t1.getTimestamp()))
                .limit(5)
                .map(TransactionMapper::mapToTransactionDto)
                .collect(Collectors.toList());

        DashboardResponse response = new DashboardResponse();
        response.setAccountBalance(account.getBalance());
        response.setTotalDeposits(totalDeposits);
        response.setTotalWithdrawals(totalWithdrawals);
        response.setRecentTransactions(recentTransactions);
        // Populate account info for UI display
        response.setAccountId(account.getId());
        response.setAccountHolderName(account.getAccountHolderName());
        response.setPhoneNumber(account.getUser() != null ? account.getUser().getPhoneNumber() : null);
        response.setAccountStatus(account.getAccountStatus() != null ? account.getAccountStatus().name() : "ACTIVE");

        return response;
    }

    @Override
    public AccountDto getMyAccount(String email) {
        Account account = accountRepository.findByUserEmail(email)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for user: " + email));
        return AccountMapper.mapToAccountDto(account);
    }
}
