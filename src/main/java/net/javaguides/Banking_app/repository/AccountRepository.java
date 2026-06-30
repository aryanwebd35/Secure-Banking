package net.javaguides.Banking_app.repository;
// ↑ This file belongs to the "repository" package — repositories talk directly to the database.

import net.javaguides.Banking_app.entity.Account;
// ↑ Imports the Account entity so the repository knows which table to work with.

import org.springframework.data.jpa.repository.JpaRepository;
// ↑ JpaRepository is a Spring Data interface that gives us FREE database methods.
//   We don't write SQL queries for basic operations — Spring Data does it for us!

import org.springframework.data.jpa.repository.Query;
// ↑ @Query allows us to write custom JPQL (Java Persistence Query Language) or SQL queries
//   when the auto-generated methods aren't enough.

import java.util.Optional;
// ↑ Optional<T> is a Java container that may or may not hold a value.
//   It's used instead of returning null when a record might not be found.
//   Usage: Optional<Account> account = repo.findByUserEmail("test@gmail.com");
//           account.isPresent() → true if found, false if not.

// ============================================================
// WHAT IS AccountRepository?
// It's the DATABASE ACCESS LAYER for Account objects.
// By extending JpaRepository<Account, Long>, we get these methods FOR FREE:
//   ✅ accountRepository.save(account)         → INSERT or UPDATE a row
//   ✅ accountRepository.findById(id)          → SELECT * FROM accounts WHERE id = ?
//   ✅ accountRepository.findAll()             → SELECT * FROM accounts
//   ✅ accountRepository.delete(account)       → DELETE FROM accounts WHERE id = ?
//   ✅ accountRepository.count()               → SELECT COUNT(*) FROM accounts
//
// We then ADD custom methods below for our specific needs.
//
// HOW DOES Spring Data KNOW the SQL?
// It reads the METHOD NAME and generates the query automatically!
//   findByUserEmail("test@gmail.com")
//   → SELECT * FROM accounts WHERE user_email = 'test@gmail.com'
//   (It joins with the users table automatically because user is a @OneToOne relationship!)
// ============================================================
public interface AccountRepository extends JpaRepository<Account, Long> {
    // ↑ "interface" means this is a contract — Spring Data provides the actual implementation.
    // extends JpaRepository<Account, Long> means:
    //   → Work with "Account" entities
    //   → The primary key type is "Long" (the id field)

    // Spring Data auto-generates the SQL: 
    // SELECT * FROM accounts JOIN users ON accounts.user_id = users.id WHERE users.phone_number = ?
    Optional<Account> findByUserPhoneNumber(String phoneNumber);
    // Used for: When a user wants to TRANSFER money, they give a phone number.
    //           We look up WHICH account belongs to that phone number.



    // Spring Data auto-generates:
    // SELECT * FROM accounts JOIN users ON ... WHERE users.email = ?
    Optional<Account> findByUserEmail(String email);
    // Used for: Finding an account using the user's email.
    //           This is the most common lookup — used after JWT login to fetch the user's account.

    // @Query → We write our own JPQL query here because Spring Data can't auto-generate SUM queries.
    // JPQL (Java Persistence Query Language) is like SQL but uses Java class names instead of table names:
    //   "Account a"  → accounts table
    //   "a.balance"  → the balance column
    //   COALESCE(X, 0.0) → if the SUM is null (no accounts), return 0.0 instead
    @Query("SELECT COALESCE(SUM(a.balance), 0.0) FROM Account a")
    Double sumAllBalances();
    // Used for: Admin dashboard to show the TOTAL money currently in the banking system.
}