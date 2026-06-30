package net.javaguides.Banking_app.repository;
// ↑ Belongs to the "repository" package — talks directly to the database.

import net.javaguides.Banking_app.entity.Transaction;
// ↑ Imports the Transaction entity so this repository works with the "transactions" table.

import org.springframework.data.domain.Page;
// ↑ Page<T> is like a "page" in a book — instead of returning ALL records at once,
//   it returns a chunk (e.g., 20 records at a time). Good for performance with large data.

import org.springframework.data.domain.Pageable;
// ↑ Pageable tells the query HOW to paginate:
//   - which page number (0 = first page, 1 = second page, etc.)
//   - how many records per page (size)
//   - sorting field and direction (e.g., sort by timestamp descending)

import org.springframework.data.jpa.repository.JpaRepository;
// ↑ Gives us FREE database methods (save, findById, findAll, count, etc.)

import org.springframework.data.jpa.repository.Query;
// ↑ @Query allows writing custom JPQL queries when auto-generated methods aren't enough.

import org.springframework.data.repository.query.Param;
// ↑ @Param binds method parameters to named parameters in the @Query string.
//   Example: @Param("accountId") Long accountId → :accountId in the query.

import java.time.LocalDateTime;
// ↑ Java's date-time class. Used for filtering transactions by start/end date range.

import java.util.List;
// ↑ Standard Java List. Used when we want ALL matching results (not paginated).

// ============================================================
// WHAT IS TransactionRepository?
// The database access layer for Transaction objects ("transactions" table).
//
// FREE methods from JpaRepository:
//   ✅ transactionRepository.save(tx)        → INSERT or UPDATE a transaction
//   ✅ transactionRepository.findById(id)    → Get one transaction by ID
//   ✅ transactionRepository.findAll(page)   → Get all transactions (paginated)
//   ✅ transactionRepository.count()         → Total number of transactions
//
// CUSTOM QUERIES BELOW: We need custom queries because transactions involve TWO accounts
// (sender AND receiver). Spring Data's auto-naming can't handle this complexity.
// ============================================================
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // ============================================================
    // CUSTOM QUERY 1: getTransactionHistory with date range filtering
    // ============================================================
    // @Query: Custom JPQL query to find transactions for a given account with optional date filters.
    //
    // JPQL BREAKDOWN:
    //   FROM Transaction t       → Work with the "transactions" table (aliased as "t")
    //   WHERE                    → Filter conditions:
    //     t.senderAccountId = :accountId OR t.receiverAccountId = :accountId
    //     → Include transactions where this account is EITHER sender OR receiver
    //     AND (:start IS NULL OR t.timestamp >= :start)
    //     → If a start date was provided, only include transactions AFTER that date
    //       If start is null (not provided), skip this filter (include all)
    //     AND (:end IS NULL OR t.timestamp <= :end)
    //     → If an end date was provided, only include transactions BEFORE that date
    //       If end is null (not provided), skip this filter (include all)
    // Pageable → Spring adds ORDER BY and LIMIT/OFFSET automatically for pagination
    @Query("SELECT t FROM Transaction t WHERE (t.senderAccountId = :accountId OR t.receiverAccountId = :accountId) " +
            "AND (:start IS NULL OR t.timestamp >= :start) AND (:end IS NULL OR t.timestamp <= :end)")
    Page<Transaction> findHistory(@Param("accountId") Long accountId,
                                  @Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end,
                                  Pageable pageable);
    // Used for: "Show me my transaction history" — on the frontend transaction history page.
    // Returns paginated results sorted by timestamp (newest first, as configured in controller).

    // ============================================================
    // CUSTOM QUERY 2: Get ALL transactions for an account (no pagination, no date filter)
    // ============================================================
    // Used for: Dashboard calculation of total deposits and total withdrawals.
    //           We need ALL transactions to calculate the running total.
    @Query("SELECT t FROM Transaction t WHERE t.senderAccountId = :accountId OR t.receiverAccountId = :accountId")
    List<Transaction> findAllByAccountId(@Param("accountId") Long accountId);
    // Returns a simple List (not Page) — all transactions for this account, unsorted.
    // Used in AccountServiceImpl.getUserDashboard() to calculate stats.
}
