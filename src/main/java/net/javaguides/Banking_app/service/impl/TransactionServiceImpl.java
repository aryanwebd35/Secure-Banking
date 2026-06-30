package net.javaguides.Banking_app.service.impl;
// ↑ "impl" sub-package — contains actual implementations of service interfaces.

import net.javaguides.Banking_app.dto.TransactionDto;
import net.javaguides.Banking_app.entity.*;
// ↑ Imports all entity classes: Account, Transaction, TransactionType, TransactionStatus, AccountStatus

import net.javaguides.Banking_app.exception.*;
// ↑ Imports all custom exceptions: AccountNotFoundException, InsufficientBalanceException, etc.

import net.javaguides.Banking_app.mapper.TransactionMapper;
import net.javaguides.Banking_app.repository.AccountRepository;
import net.javaguides.Banking_app.repository.TransactionRepository;
import net.javaguides.Banking_app.service.TransactionService;

import lombok.extern.slf4j.Slf4j;
// ↑ @Slf4j from Lombok: automatically creates a "log" variable.
//   Allows us to print structured log messages: log.info(), log.warn(), log.error()

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
// ↑ Page<T> and Pageable for paginated query results.

import org.springframework.stereotype.Service;
// ↑ @Service marks this as a Spring-managed service bean.

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
// ↑ @Transactional is very important for banking operations:
//   - It wraps a method in a DATABASE TRANSACTION.
//   - If ANYTHING fails inside the method, ALL database changes are ROLLED BACK (undone).
//   - This ensures your data is never in a "half-complete" state.
//   - ANALOGY: Like a bank vault — either ALL the money moves, or NONE does.

import java.time.LocalDateTime;

// ============================================================
// WHAT IS TransactionServiceImpl?
// The CORE of this banking application — handles ALL money movements.
//
// HOW @Transactional WORKS (very important to understand!):
// ┌─────────────────────────────────────────────────────────┐
// │  @Transactional method starts                           │
// │  ├── Step 1: Deduct from sender ✅                      │
// │  ├── Step 2: Add to receiver ✅                         │
// │  ├── Step 3: Save transaction record... ❌ (ERROR!)     │
// │  └── ROLLBACK: Step 1 and Step 2 are UNDONE automatically│
// └─────────────────────────────────────────────────────────┘
// Without @Transactional, if Step 3 fails, sender loses money but receiver doesn't get it!
//
// PROPAGATION.REQUIRES_NEW:
// Creates a SEPARATE, INDEPENDENT transaction.
// Used for saving "failed transaction" records — because even if the main transaction
// rolls back (undoing balance changes), we STILL want to save the failure record.
// ============================================================

// @Service → Register this class as a Spring service bean.
// @Slf4j → Create a "log" variable automatically for logging.
@Service
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository; // Talks to "transactions" table
    private final AccountRepository accountRepository;         // Talks to "accounts" table

    // Constructor injection — Spring provides these dependencies automatically.
    public TransactionServiceImpl(TransactionRepository transactionRepository, AccountRepository accountRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    // ============================================================
    // DEPOSIT: Add money to an account
    // ============================================================
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    // ↑ Each deposit runs in its OWN separate database transaction.
    //   If this deposit fails, only this transaction rolls back (not the caller's transaction).
    public TransactionDto deposit(Long accountId, double amount) {
        log.info("Processing Deposit of amount {} to account ID {}", amount, accountId);
        // log.info() prints to console: "Processing Deposit of amount 500.0 to account ID 3"

        // VALIDATION: Deposit amount must be positive.
        if (amount <= 0) {
            throw new InvalidTransactionException("Deposit amount must be greater than zero");
        }

        // Find the account in the database. If not found, throw a clear error.
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for ID: " + accountId));

        // Check if account is ACTIVE (not blocked or closed).
        validateAccountStatus(account, "deposit");

        // Add the deposit amount to the current balance.
        account.setBalance(account.getBalance() + amount);
        accountRepository.save(account); // Save the updated balance to database.

        // Create a new Transaction record to log this deposit.
        Transaction transaction = new Transaction();
        transaction.setTransactionType(TransactionType.DEPOSIT); // Type = DEPOSIT
        transaction.setAmount(amount);                           // The deposited amount
        transaction.setReceiverAccountId(accountId);             // This account received the money
        // Note: senderAccountId is null for deposits (no sender — money comes from outside)
        transaction.setStatus(TransactionStatus.SUCCESS);        // Mark as successful
        transaction.setTimestamp(LocalDateTime.now());           // Record exact time
        transaction.setRemarks("Deposit transaction");           // Default note
        Transaction saved = transactionRepository.save(transaction); // Save to DB

        log.info("Deposit successful. Transaction ID: {}", saved.getId());
        // Convert the saved Transaction entity → TransactionDto and return it.
        return TransactionMapper.mapToTransactionDto(saved);
    }

    // ============================================================
    // WITHDRAWAL: Remove money from an account
    // ============================================================
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    // ↑ Each withdrawal is its own independent database transaction.
    public TransactionDto withdraw(Long accountId, double amount) {
        log.info("Processing Withdrawal of amount {} from account ID {}", amount, accountId);

        // VALIDATION: Amount must be positive.
        if (amount <= 0) {
            throw new InvalidTransactionException("Withdrawal amount must be greater than zero");
        }

        // Find the account.
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for ID: " + accountId));

        // Check if account is ACTIVE.
        validateAccountStatus(account, "withdrawal");

        // VALIDATION: Check if there's enough money.
        if (account.getBalance() < amount) {
            // Save a FAILED transaction record BEFORE throwing the error.
            // This records the attempt even though it fails.
            saveFailedTransaction(TransactionType.WITHDRAW, amount, accountId, null, "Insufficient balance");
            throw new InsufficientBalanceException("Insufficient balance in account ID: " + accountId);
        }

        // Deduct the withdrawal amount from balance.
        account.setBalance(account.getBalance() - amount);
        accountRepository.save(account); // Save updated balance to DB.

        // Create and save a Transaction record.
        Transaction transaction = new Transaction();
        transaction.setTransactionType(TransactionType.WITHDRAW); // Type = WITHDRAW
        transaction.setAmount(amount);
        transaction.setSenderAccountId(accountId);  // This account sent (gave up) the money
        // Note: receiverAccountId is null (money goes to external world, like cash)
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setRemarks("Withdrawal transaction");
        Transaction saved = transactionRepository.save(transaction);

        log.info("Withdrawal successful. Transaction ID: {}", saved.getId());
        return TransactionMapper.mapToTransactionDto(saved);
    }

    // ============================================================
    // TRANSFER: Move money from one user to another by phone number
    // ============================================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    // ↑ @Transactional(rollbackFor = Exception.class):
    //   If ANY exception is thrown inside this method, ALL database changes are rolled back.
    //   This ensures money is NEVER lost: if the transaction fails halfway,
    //   the sender gets their money back (balance unchanged).
    public TransactionDto transfer(String senderEmail, String receiverPhoneNumber, double amount, String remarks) {
        log.info("Initiating transfer of amount {} from user {} to phone number {}", amount, senderEmail, receiverPhoneNumber);

        // Step 1: Find SENDER's account by email.
        Account senderAccount = accountRepository.findByUserEmail(senderEmail)
                .orElseThrow(() -> new AccountNotFoundException("Sender account not found for email: " + senderEmail));

        // Step 2: Find RECEIVER's account by phone number.
        Account receiverAccount = accountRepository.findByUserPhoneNumber(receiverPhoneNumber)
                .orElseThrow(() -> new AccountNotFoundException("Receiver account not found for phone: " + receiverPhoneNumber));

        // Step 3: Prevent self-transfer (sending money to your own account).
        if (senderAccount.getId().equals(receiverAccount.getId())) {
            throw new InvalidTransactionException("Cannot transfer to the same account");
        }

        try {
            // Step 4: Validate both accounts are ACTIVE.
            validateAccountStatus(senderAccount, "transfer out");
            validateAccountStatus(receiverAccount, "transfer in");

            // Step 5: Check sender has enough money.
            if (senderAccount.getBalance() < amount) {
                throw new InsufficientBalanceException("Insufficient balance for transfer");
            }

            // Step 6: PERFORM THE TRANSFER — deduct from sender, add to receiver.
            senderAccount.setBalance(senderAccount.getBalance() - amount); // Deduct
            receiverAccount.setBalance(receiverAccount.getBalance() + amount); // Credit

            // Step 7: Save BOTH updated balances to the database.
            accountRepository.save(senderAccount);
            accountRepository.save(receiverAccount);

            // Step 8: Create a single transaction record for this transfer.
            Transaction transaction = new Transaction();
            transaction.setTransactionType(TransactionType.TRANSFER);    // Type = TRANSFER
            transaction.setAmount(amount);
            transaction.setSenderAccountId(senderAccount.getId());        // Who sent
            transaction.setReceiverAccountId(receiverAccount.getId());    // Who received
            transaction.setStatus(TransactionStatus.SUCCESS);
            transaction.setTimestamp(LocalDateTime.now());
            transaction.setRemarks(remarks != null ? remarks : "Fund transfer"); // Use provided note or default
            Transaction saved = transactionRepository.save(transaction);

            log.info("Transfer completed successfully. Transaction ID: {}", saved.getId());
            return TransactionMapper.mapToTransactionDto(saved);

        } catch (Exception e) {
            // If anything went wrong:
            log.error("Transfer failed: {}. Rolling back balances and logging failed transaction record.", e.getMessage());

            // Save a FAILED transaction record using a SEPARATE transaction (Propagation.REQUIRES_NEW).
            // The main transaction will ROLLBACK (undo balance changes), but this failed record
            // uses its own separate transaction so it gets COMMITTED even when main rolls back.
            saveFailedTransaction(TransactionType.TRANSFER, amount, senderAccount.getId(), receiverAccount.getId(), e.getMessage());

            throw e; // Re-throw the exception to trigger the main transaction's rollback.
        }
    }

    // ============================================================
    // GET TRANSACTION HISTORY (paginated, with optional date filter)
    // ============================================================
    @Override
    public Page<TransactionDto> getTransactionHistory(String userEmail, LocalDateTime start, LocalDateTime end, Pageable pageable) {
        // First, find the account for this email.
        Account account = accountRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new AccountNotFoundException("Account not found for email: " + userEmail));

        // Then query the transaction table using the custom findHistory() query from TransactionRepository.
        // start and end can be null (no date filter) — the JPQL query handles this.
        return transactionRepository.findHistory(account.getId(), start, end, pageable)
                .map(TransactionMapper::mapToTransactionDto);
        // .map() converts each Transaction entity in the page → TransactionDto
    }

    // ============================================================
    // GET ONE TRANSACTION BY ID
    // ============================================================
    @Override
    public TransactionDto getTransactionById(Long id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new InvalidTransactionException("Transaction not found for ID: " + id));
        return TransactionMapper.mapToTransactionDto(transaction);
    }

    // ============================================================
    // GET ALL TRANSACTIONS (Admin feature, paginated)
    // ============================================================
    @Override
    public Page<TransactionDto> getAllTransactions(Pageable pageable) {
        // findAll(pageable) from JpaRepository returns a Page<Transaction>.
        // .map() converts each Transaction → TransactionDto.
        return transactionRepository.findAll(pageable)
                .map(TransactionMapper::mapToTransactionDto);
    }

    // ============================================================
    // PRIVATE HELPER: Check if account can perform operations
    // ============================================================
    // "private" means this method is ONLY usable INSIDE this class.
    // It's a helper method to avoid repeating the same check in deposit/withdraw/transfer.
    private void validateAccountStatus(Account account, String operation) {
        // CLOSED accounts cannot do ANYTHING — ever.
        if (account.getAccountStatus() == AccountStatus.CLOSED) {
            throw new AccountBlockedException("Account is CLOSED. Inaccessible for " + operation);
        }
        // BLOCKED accounts are temporarily frozen — no transactions allowed.
        if (account.getAccountStatus() == AccountStatus.BLOCKED) {
            throw new AccountBlockedException("Account is BLOCKED. Cannot perform " + operation);
        }
        // If neither CLOSED nor BLOCKED → account is ACTIVE → proceed normally.
    }

    // ============================================================
    // PRIVATE HELPER: Save a record of a FAILED transaction
    // ============================================================
    // @Transactional(propagation = Propagation.REQUIRES_NEW):
    //   This method runs in its OWN separate database transaction.
    //   Even if the calling method's transaction rolls back,
    //   this failure record will still be committed to the database.
    //   This ensures we have a complete audit trail of ALL attempts, including failures.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailedTransaction(TransactionType type, double amount, Long senderId, Long receiverId, String remarks) {
        Transaction transaction = new Transaction();
        transaction.setTransactionType(type);          // DEPOSIT, WITHDRAW, or TRANSFER
        transaction.setAmount(amount);                 // The attempted amount
        transaction.setSenderAccountId(senderId);      // Who tried to send (can be null for deposits)
        transaction.setReceiverAccountId(receiverId);  // Who was supposed to receive (can be null)
        transaction.setStatus(TransactionStatus.FAILED); // Mark as FAILED
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setRemarks("FAILED: " + remarks); // Prefix "FAILED:" to the error message
        transactionRepository.save(transaction);       // Commit this failure record to DB
    }
}
