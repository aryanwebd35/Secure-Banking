package net.javaguides.Banking_app.service.impl;
// ↑ This file is in the "impl" sub-package. "impl" is short for "implementation."
//   Service interfaces define WHAT to do. This class defines HOW.

import net.javaguides.Banking_app.dto.RegisterRequest;
import net.javaguides.Banking_app.dto.UserDto;
import net.javaguides.Banking_app.entity.Account;
import net.javaguides.Banking_app.entity.AccountStatus;
import net.javaguides.Banking_app.entity.Role;
import net.javaguides.Banking_app.entity.User;
import net.javaguides.Banking_app.exception.UserNotFoundException;
import net.javaguides.Banking_app.mapper.UserMapper;
import net.javaguides.Banking_app.repository.AccountRepository;
import net.javaguides.Banking_app.repository.RoleRepository;
import net.javaguides.Banking_app.repository.UserRepository;
import net.javaguides.Banking_app.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
// ↑ @Transactional ensures database operations are atomic (all-or-nothing).
//   If anything fails inside a @Transactional method, ALL database changes are rolled back.

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

// ============================================================
// WHAT IS UserServiceImpl?
// Implementation of UserService. Handles:
//   1. User REGISTRATION (one-step: create user + bank account atomically)
//   2. User LOOKUP for Spring Security authentication (loadUserByUsername)
//   3. User LISTING for admin dashboard (paginated)
//
// USER REGISTRATION FLOW:
//   1. POST /api/auth/register with { name, email, password, phoneNumber }
//   2. AuthController calls userService.register(request)
//   3. THIS class validates email uniqueness and phone uniqueness
//   4. Hashes the password with BCrypt
//   5. Creates the User entity and saves it
//   6. Creates a linked bank Account and saves it (ACTIVE, balance $0)
//   7. Returns the UserDto (safe view — no password)
//   8. AuthController generates a JWT and returns it to the client
//
// LOGIN FLOW:
//   1. POST /api/auth/login with { email, password }
//   2. Spring Security's AuthenticationManager calls loadUserByUsername(email)
//   3. THIS method fetches the User from DB and wraps it in a UserDetails object
//   4. Spring Security compares the submitted password against the stored BCrypt hash
//   5. If valid → AuthController generates a JWT and returns it
// ============================================================

// @Service → Spring registers this class as a service bean.
// @Slf4j → Lombok: auto-creates a log variable for structured logging.
@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;       // Talks to the "users" table
    private final RoleRepository roleRepository;       // Talks to the "roles" table
    private final AccountRepository accountRepository; // Talks to the "accounts" table
    private final PasswordEncoder passwordEncoder;     // BCrypt hashing and verification

    // Constructor injection — Spring automatically provides all dependencies.
    public UserServiceImpl(UserRepository userRepository,
                           RoleRepository roleRepository,
                           AccountRepository accountRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ============================================================
    // LOAD USER BY USERNAME (Required by Spring Security's UserDetailsService)
    // Called automatically by Spring Security's AuthenticationManager during login.
    // "username" in our app = the user's email address.
    // ============================================================
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.info("Loading user by email for authentication: {}", email);

        // Find the user by email. If not found, throw UsernameNotFoundException.
        // Spring Security catches this exception and returns HTTP 401 Unauthorized.
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("No user found with email: " + email));

        // Convert our Role entities into Spring Security GrantedAuthority objects.
        // Example: Role("ROLE_USER") → SimpleGrantedAuthority("ROLE_USER")
        List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .toList();

        // Return a Spring Security UserDetails object.
        // Spring Security uses this to: compare passwords, check account status, and set roles.
        // org.springframework.security.core.userdetails.User is Spring's built-in UserDetails impl.
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),    // The "username" (email in our case)
                user.getPassword(), // The BCrypt hash — Spring will compare against the submitted password
                authorities         // The list of roles/authorities
        );
    }

    // ============================================================
    // REGISTER (One-step: creates user + bank account atomically)
    // Called by AuthController when POST /api/auth/register is received.
    // ============================================================
    @Override
    @Transactional
    // ↑ @Transactional: if saving the user succeeds but creating the account fails,
    //   BOTH changes are rolled back. We never want a user without a bank account.
    public UserDto register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        // ── Step 1: Validate email uniqueness ────────────────────────────────
        // We check before inserting to give a clear error message.
        // Without this check, MySQL would throw a cryptic constraint violation.
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("An account with this email already exists: " + request.getEmail());
        }

        // ── Step 2: Validate phone number uniqueness ──────────────────────────
        // Phone numbers must be unique because they are used as transfer identifiers.
        if (userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new IllegalArgumentException("This phone number is already registered with another account.");
        }

        // ── Step 3: Ensure ROLE_USER exists in the database ──────────────────
        // "find or create" pattern: if the role doesn't exist yet, create it.
        // This handles fresh databases where roles haven't been seeded yet.
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new Role(null, "ROLE_USER")));

        // ── Step 4: Build the User entity ────────────────────────────────────
        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        // Hash the plain-text password with BCrypt before storing.
        // BCrypt is a one-way function — the plain password can NEVER be recovered from this hash.
        // Example: "password123" → "$2a$10$jDt/..."
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhoneNumber(request.getPhoneNumber());
        // Assign the ROLE_USER role to this new user.
        user.setRoles(new HashSet<>(Collections.singletonList(userRole)));

        // ── Step 5: Save the user to the database ────────────────────────────
        user = userRepository.save(user); // INSERT INTO users (name, email, password, phone_number, ...) VALUES (...)
        log.info("Created new user ID: {}", user.getId());

        // ── Step 6: Create the bank account for this user ────────────────────
        // The bank account is created immediately during registration (not in a separate step).
        Account account = new Account();
        account.setAccountHolderName(user.getName()); // Account holder = user's name
        account.setBalance(0.0);                      // New accounts start with zero balance
        account.setUser(user);                        // Link the account to this user (FK)
        account.setAccountStatus(AccountStatus.ACTIVE); // All new accounts are ACTIVE by default
        accountRepository.save(account); // INSERT INTO accounts (account_holder_name, balance, user_id, ...) VALUES (...)
        log.info("Created bank account for user ID: {} with phone: {}", user.getId(), request.getPhoneNumber());

        // ── Step 7: Return the user info (safe DTO — no password) ────────────
        return UserMapper.mapToUserDto(user);
    }

    // ============================================================
    // GET ALL USERS (Admin feature, paginated)
    // Called by AdminController for the admin user management dashboard.
    // ============================================================
    @Override
    public Page<UserDto> getAllUsers(Pageable pageable) {
        // findAll(pageable) → paginated SELECT * FROM users LIMIT ? OFFSET ?
        // .map(UserMapper::mapToUserDto) → convert each User entity to a safe UserDto
        return userRepository.findAll(pageable)
                .map(UserMapper::mapToUserDto);
    }
}
