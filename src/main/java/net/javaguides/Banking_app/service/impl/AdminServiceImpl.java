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

@Service
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public AdminServiceImpl(UserRepository userRepository, AccountRepository accountRepository, TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public Page<UserDto> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(UserMapper::mapToUserDto);
    }

    @Override
    public Page<AccountDto> getAllAccounts(Pageable pageable) {
        return accountRepository.findAll(pageable)
                .map(AccountMapper::mapToAccountDto);
    }

    @Override
    @Transactional
    public AccountDto updateAccountStatus(Long accountId, String status) {
        log.info("Admin updating account ID {} status to {}", accountId, status);
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for ID: " + accountId));

        AccountStatus targetStatus = AccountStatus.valueOf(status.toUpperCase());
        account.setAccountStatus(targetStatus);
        Account saved = accountRepository.save(account);
        return AccountMapper.mapToAccountDto(saved);
    }

    @Override
    public Map<String, Object> getAdminDashboardStats() {
        log.info("Fetching Admin Dashboard Statistics");
        Map<String, Object> stats = new HashMap<>();

        // Total money in system
        Double totalMoney = accountRepository.sumAllBalances();
        stats.put("totalMoneyInSystem", totalMoney);

        // Top 5 account holders
        Pageable topFive = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "balance"));
        List<AccountDto> topAccountHolders = accountRepository.findAll(topFive)
                .getContent()
                .stream()
                .map(AccountMapper::mapToAccountDto)
                .collect(Collectors.toList());
        stats.put("topAccountHolders", topAccountHolders);

        // Transaction statistics
        long totalTransactions = transactionRepository.count();
        stats.put("totalTransactionsCount", totalTransactions);

        // Total users & accounts
        stats.put("totalUsersCount", userRepository.count());
        stats.put("totalAccountsCount", accountRepository.count());

        return stats;
    }
}
