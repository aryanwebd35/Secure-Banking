package net.javaguides.Banking_app.repository;

import net.javaguides.Banking_app.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT t FROM Transaction t WHERE (t.senderAccountId = :accountId OR t.receiverAccountId = :accountId) " +
            "AND (:start IS NULL OR t.timestamp >= :start) AND (:end IS NULL OR t.timestamp <= :end)")
    Page<Transaction> findHistory(@Param("accountId") Long accountId,
                                  @Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end,
                                  Pageable pageable);

    @Query("SELECT t FROM Transaction t WHERE t.senderAccountId = :accountId OR t.receiverAccountId = :accountId")
    List<Transaction> findAllByAccountId(@Param("accountId") Long accountId);
}
