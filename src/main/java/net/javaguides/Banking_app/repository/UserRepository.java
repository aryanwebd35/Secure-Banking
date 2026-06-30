package net.javaguides.Banking_app.repository;
// ↑ Belongs to the "repository" package — talks directly to the database.

import net.javaguides.Banking_app.entity.User;
// ↑ Imports the User entity so this repository knows it works with the "users" table.

import org.springframework.data.jpa.repository.JpaRepository;
// ↑ JpaRepository gives us FREE database methods (save, findById, findAll, delete, count, etc.)

import java.util.Optional;
// ↑ Optional<T> = a container that might or might not have a value.
//   Safe alternative to returning null.

// ============================================================
// WHAT IS UserRepository?
// The database access layer for User objects (the "users" table).
//
// FREE methods from JpaRepository:
//   ✅ userRepository.save(user)            → INSERT or UPDATE in users table
//   ✅ userRepository.findById(id)          → SELECT * FROM users WHERE id = ?
//   ✅ userRepository.findAll()             → SELECT * FROM users
//   ✅ userRepository.delete(user)          → DELETE FROM users WHERE id = ?
//   ✅ userRepository.count()               → SELECT COUNT(*) FROM users
//
// NAMING CONVENTION for custom methods:
//   Spring Data reads the method name and auto-generates SQL!
//   findByEmail("x@gmail.com")        → SELECT * FROM users WHERE email = 'x@gmail.com'
//   existsByEmail("x@gmail.com")      → SELECT COUNT(*) > 0 FROM users WHERE email = ?
//   existsByPhoneNumber("9876543210") → SELECT COUNT(*) > 0 FROM users WHERE phone_number = ?
// ============================================================
public interface UserRepository extends JpaRepository<User, Long> {
    // ↑ Works with "User" entities and "Long" primary key type.

    // Auto-SQL: SELECT * FROM users WHERE email = ?
    // Used for: Loading user by email for login authentication.
    //           Spring Security's UserDetailsService calls this.
    Optional<User> findByEmail(String email);

    // Auto-SQL: SELECT * FROM users WHERE phone_number = ?
    // Used for: Looking up the receiver's account during a money transfer.
    Optional<User> findByPhoneNumber(String phoneNumber);

    // Auto-SQL: SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END FROM users WHERE email = ?
    // Used for: Checking if an email is already taken during registration.
    //           Returns true if the email exists (duplicate), false if it's new (OK to register).
    boolean existsByEmail(String email);

    // Auto-SQL: SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END FROM users WHERE phone_number = ?
    // Used for: Checking if a phone number is already registered during registration.
    //           Prevents two users from having the same phone number (required for transfers).
    boolean existsByPhoneNumber(String phoneNumber);
}
