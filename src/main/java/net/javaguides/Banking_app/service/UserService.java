package net.javaguides.Banking_app.service;
// ↑ Belongs to the "service" package.

import net.javaguides.Banking_app.dto.RegisterRequest;
import net.javaguides.Banking_app.dto.UserDto;
// ↑ Imports DTOs used as input/output for service methods.

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
// ↑ Page<T> and Pageable are Spring Data tools for paginated database queries.
//   Instead of returning all 10,000 users at once, we return 20 per page.

import org.springframework.security.core.userdetails.UserDetailsService;
// ↑ UserDetailsService is a core Spring Security interface.
//   It has ONE method: loadUserByUsername(String username)
//   Spring Security calls this during login to load the user from the database.

// ============================================================
// WHAT IS UserService?
// An interface defining user management operations.
// The actual implementation lives in UserServiceImpl.java.
//
// This service handles:
//   - Registering a new user (creates the user + bank account in one step)
//   - Loading a user by email (required by Spring Security for JWT authentication)
//   - Listing all users (admin-only feature)
//
// WHY EXTEND UserDetailsService?
// Spring Security needs to know how to look up users during authentication.
// By implementing UserDetailsService here, we give Spring Security access to
// our user database so it can verify credentials at login time.
// ============================================================
public interface UserService extends UserDetailsService {
    // ↑ Extending UserDetailsService means this interface also includes:
    //   UserDetails loadUserByUsername(String username) throws UsernameNotFoundException;
    //   Spring Security calls loadUserByUsername(email) during every login attempt.

    // Register a brand new user: validate uniqueness → hash password → save user → create bank account.
    // All done in a single atomic transaction.
    // Throws IllegalArgumentException if email or phone number is already taken.
    UserDto register(RegisterRequest request);

    // Get a paginated list of all users (admin-only feature).
    // pageable controls page size, page number, and sorting.
    Page<UserDto> getAllUsers(Pageable pageable);
}
