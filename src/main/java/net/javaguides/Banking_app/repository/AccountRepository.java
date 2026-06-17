package net.javaguides.Banking_app.repository;

import net.javaguides.Banking_app.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByUserPhoneNumber(String phoneNumber);
    Optional<Account> findByUserClerkUserId(String clerkUserId);
    Optional<Account> findByUserEmail(String email);

    @Query("SELECT COALESCE(SUM(a.balance), 0.0) FROM Account a")
    Double sumAllBalances();
}