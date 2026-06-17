package net.javaguides.Banking_app.service.impl;

import net.javaguides.Banking_app.dto.TransactionDto;
import net.javaguides.Banking_app.entity.*;
import net.javaguides.Banking_app.exception.*;
import net.javaguides.Banking_app.mapper.TransactionMapper;
import net.javaguides.Banking_app.repository.AccountRepository;
import net.javaguides.Banking_app.repository.TransactionRepository;
import net.javaguides.Banking_app.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public TransactionServiceImpl(TransactionRepository transactionRepository, AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TransactionDto deposit(Long accountId, double amount) {
        log.info("Processing Deposit of amount {} to account ID {}", amount, accountId);
        if (amount <= 0) {
            throw new InvalidTransactionException("Deposit amount must be greater than zero");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for ID: " + accountId));

        validateAccountStatus(account, "deposit");

        account.setBalance(account.getBalance() + amount);
        accountRepository.save(account);

        Transaction transaction = new Transaction();
        transaction.setTransactionType(TransactionType.DEPOSIT);
        transaction.setAmount(amount);
        transaction.setReceiverAccountId(accountId);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setRemarks("Deposit transaction");
        Transaction saved = transactionRepository.save(transaction);

        log.info("Deposit successful. Transaction ID: {}", saved.getId());
        return TransactionMapper.mapToTransactionDto(saved);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TransactionDto withdraw(Long accountId, double amount) {
        log.info("Processing Withdrawal of amount {} from account ID {}", amount, accountId);
        if (amount <= 0) {
            throw new InvalidTransactionException("Withdrawal amount must be greater than zero");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for ID: " + accountId));

        validateAccountStatus(account, "withdrawal");

        if (account.getBalance() < amount) {
            saveFailedTransaction(TransactionType.WITHDRAW, amount, accountId, null, "Insufficient balance");
            throw new InsufficientBalanceException("Insufficient balance in account ID: " + accountId);
        }

        account.setBalance(account.getBalance() - amount);
        accountRepository.save(account);

        Transaction transaction = new Transaction();
        transaction.setTransactionType(TransactionType.WITHDRAW);
        transaction.setAmount(amount);
        transaction.setSenderAccountId(accountId);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setRemarks("Withdrawal transaction");
        Transaction saved = transactionRepository.save(transaction);

        log.info("Withdrawal successful. Transaction ID: {}", saved.getId());
        return TransactionMapper.mapToTransactionDto(saved);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TransactionDto transfer(String senderEmail, String receiverPhoneNumber, double amount, String remarks) {
        log.info("Initiating transfer of amount {} from user {} to phone number {}", amount, senderEmail, receiverPhoneNumber);

        Account senderAccount = accountRepository.findByUserEmail(senderEmail)
                .orElseThrow(() -> new AccountNotFoundException("Sender account not found for email: " + senderEmail));

        Account receiverAccount = accountRepository.findByUserPhoneNumber(receiverPhoneNumber)
                .orElseThrow(() -> new AccountNotFoundException("Receiver account not found for phone: " + receiverPhoneNumber));

        if (senderAccount.getId().equals(receiverAccount.getId())) {
            throw new InvalidTransactionException("Cannot transfer to the same account");
        }

        try {
            validateAccountStatus(senderAccount, "transfer out");
            validateAccountStatus(receiverAccount, "transfer in");

            if (senderAccount.getBalance() < amount) {
                throw new InsufficientBalanceException("Insufficient balance for transfer");
            }

            // Perform transfer
            senderAccount.setBalance(senderAccount.getBalance() - amount);
            receiverAccount.setBalance(receiverAccount.getBalance() + amount);

            accountRepository.save(senderAccount);
            accountRepository.save(receiverAccount);

            Transaction transaction = new Transaction();
            transaction.setTransactionType(TransactionType.TRANSFER);
            transaction.setAmount(amount);
            transaction.setSenderAccountId(senderAccount.getId());
            transaction.setReceiverAccountId(receiverAccount.getId());
            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setTimestamp(LocalDateTime.now());
            transaction.setRemarks(remarks != null ? remarks : "Fund transfer");
            Transaction saved = transactionRepository.save(transaction);

            log.info("Transfer completed successfully. Transaction ID: {}", saved.getId());
            return TransactionMapper.mapToTransactionDto(saved);

        } catch (Exception e) {
            log.error("Transfer failed: {}. Rolling back balances and logging failed transaction record.", e.getMessage());
            // Log the failure in database (using a separate transaction so it's committed despite rollback of balance changes)
            saveFailedTransaction(TransactionType.TRANSFER, amount, senderAccount.getId(), receiverAccount.getId(), e.getMessage());
            throw e; // Rethrow to trigger rollback
        }
    }

    @Override
    public Page<TransactionDto> getTransactionHistory(String userEmail, LocalDateTime start, LocalDateTime end, Pageable pageable) {
        Account account = accountRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for email: " + userEmail));
        return transactionRepository.findHistory(account.getId(), start, end, pageable)
                .map(TransactionMapper::mapToTransactionDto);
    }

    @Override
    public TransactionDto getTransactionById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new InvalidTransactionException("Transaction not found for ID: " + id));
        return TransactionMapper.mapToTransactionDto(transaction);
    }

    @Override
    public Page<TransactionDto> getAllTransactions(Pageable pageable) {
        return transactionRepository.findAll(pageable)
                .map(TransactionMapper::mapToTransactionDto);
    }

    private void validateAccountStatus(Account account, String operation) {
        if (account.getAccountStatus() == AccountStatus.CLOSED) {
            throw new AccountBlockedException("Account is CLOSED. Inaccessible for " + operation);
        }
        if (account.getAccountStatus() == AccountStatus.BLOCKED) {
            throw new AccountBlockedException("Account is BLOCKED. Cannot perform " + operation);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailedTransaction(TransactionType type, double amount, Long senderId, Long receiverId, String remarks) {
        Transaction transaction = new Transaction();
        transaction.setTransactionType(type);
        transaction.setAmount(amount);
        transaction.setSenderAccountId(senderId);
        transaction.setReceiverAccountId(receiverId);
        transaction.setStatus(TransactionStatus.FAILED);
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setRemarks("FAILED: " + remarks);
        transactionRepository.save(transaction);
    }
}
