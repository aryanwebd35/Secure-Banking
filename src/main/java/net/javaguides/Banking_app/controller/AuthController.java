package net.javaguides.Banking_app.controller;
// ↑ Belongs to the "controller" package.

import net.javaguides.Banking_app.dto.AuthRequest;
import net.javaguides.Banking_app.dto.AuthResponse;
import net.javaguides.Banking_app.dto.RegisterRequest;
import net.javaguides.Banking_app.dto.UserDto;
import net.javaguides.Banking_app.security.JwtTokenProvider;
import net.javaguides.Banking_app.service.UserService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
// ↑ @Value reads configuration values from application.properties.

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// ↑ AuthenticationManager.authenticate() is called with this token to verify credentials.
//   Spring Security internally calls UserService.loadUserByUsername() + BCrypt compare.

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
// ↑ PasswordEncoder.matches(rawPassword, storedHash) → safely compares a plain-text password
//   against a BCrypt hash. Used for admin login.

import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.List;

// ============================================================
// WHAT IS AuthController?
// Handles all authentication-related HTTP requests.
// All these endpoints are PUBLIC (no JWT required to access them).
// After successful authentication, they return a JWT token for future requests.
//
// ENDPOINTS:
//   POST /api/auth/register      → New user registers (name + email + password + phone)
//   POST /api/auth/login         → Existing user logs in (email + password) → returns JWT
//   POST /api/auth/admin/login   → Admin logs in (email + password from application.properties)
//
// HOW THE AUTH FLOWS WORK:
// ┌─────────────────────────────────────────────────────────────────────┐
// │ USER REGISTRATION FLOW:                                             │
// │   POST /api/auth/register                                           │
// │   Body: { "name": "Aryan", "email": "aryan@gmail.com",             │
// │           "password": "pass123", "phoneNumber": "9876543210" }      │
// │   → Validates email & phone uniqueness                              │
// │   → Hashes password with BCrypt                                     │
// │   → Creates user + bank account in one step                         │
// │   → Returns JWT with ROLE_USER                                      │
// ├─────────────────────────────────────────────────────────────────────┤
// │ USER LOGIN FLOW:                                                     │
// │   POST /api/auth/login                                              │
// │   Body: { "email": "aryan@gmail.com", "password": "pass123" }      │
// │   → Spring Security verifies credentials via AuthenticationManager  │
// │   → Returns JWT with ROLE_USER                                      │
// ├─────────────────────────────────────────────────────────────────────┤
// │ ADMIN LOGIN FLOW:                                                    │
// │   POST /api/auth/admin/login                                        │
// │   Body: { "email": "admin@bank.com", "password": "adminPassword123"}│
// │   → Checks against app.admin.email + app.admin.password (BCrypt)    │
// │   → Returns JWT with ROLE_ADMIN                                      │
// └─────────────────────────────────────────────────────────────────────┘
// ============================================================

// @RestController → Handles HTTP requests, returns JSON.
// @RequestMapping("/api/auth") → All endpoints start with "/api/auth".
// @Slf4j → Creates log variable.
@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;    // Creates our app's JWT tokens
    private final UserService userService;              // Registration and user management
    private final PasswordEncoder passwordEncoder;      // BCrypt password comparison
    private final AuthenticationManager authenticationManager;
    // ↑ Spring Security's central login component. Delegates to DaoAuthenticationProvider,
    //   which calls UserService.loadUserByUsername() + BCrypt comparison.

    // @Value(\"${app.admin.email}\") → reads "admin@bank.com" from application.properties.
    @Value("${app.admin.email}")
    private String adminEmail;

    // @Value(\"${app.admin.password}\") → reads the BCrypt hash of admin password from application.properties.
    @Value("${app.admin.password}")
    private String adminPasswordHash;

    // Constructor injection — Spring provides all dependencies automatically.
    public AuthController(JwtTokenProvider jwtTokenProvider,
                          UserService userService,
                          PasswordEncoder passwordEncoder,
                          AuthenticationManager authenticationManager) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    // ─── POST /api/auth/register ──────────────────────────────────────────
    // Registers a new user and immediately returns a JWT so they are logged in.
    // @Valid → triggers validation of RegisterRequest fields (email format, phone 10 digits, etc.)
    // @RequestBody → reads the JSON body and converts it to a RegisterRequest Java object.
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Registration attempt for email: {}", request.getEmail());

        try {
            // Register the user: validate uniqueness → hash password → create user + bank account.
            // Throws IllegalArgumentException if email or phone is already taken.
            UserDto userDto = userService.register(request);

            // Build an Authentication object to generate the JWT.
            // Since we just registered, we know the role is ROLE_USER.
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    userDto.getEmail(), null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

            // Generate the JWT token for the newly registered user.
            String token = jwtTokenProvider.generateToken(authentication);

            log.info("Registration successful for email: {}", request.getEmail());
            // Return HTTP 201 Created with the JWT + email + roles.
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new AuthResponse(token, userDto.getEmail(), List.of("ROLE_USER")));

        } catch (IllegalArgumentException e) {
            // This happens if email or phone number is already taken.
            log.warn("Registration failed for email {}: {}", request.getEmail(), e.getMessage());
            // Return HTTP 409 Conflict with the error message as a JSON string.
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    // ─── POST /api/auth/login ─────────────────────────────────────────────
    // Logs in an existing user with email + password. Returns a JWT on success.
    // @Valid → triggers validation of AuthRequest fields (email format, password not blank).
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        try {
            // authenticationManager.authenticate() does the full login check:
            //   1. Calls UserService.loadUserByUsername(email) to fetch user from DB
            //   2. Compares the submitted password against the stored BCrypt hash
            //   3. If valid → returns an authenticated Authentication object
            //   4. If invalid → throws BadCredentialsException
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );

            // Generate a JWT for the authenticated user.
            // authentication.getAuthorities() contains the user's roles from the DB.
            String token = jwtTokenProvider.generateToken(authentication);

            // Extract the role strings for the response body.
            List<String> roles = authentication.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .toList();

            log.info("Login successful for email: {}", request.getEmail());
            return ResponseEntity.ok(new AuthResponse(token, request.getEmail(), roles));

        } catch (BadCredentialsException e) {
            // Thrown when the email doesn't exist OR the password doesn't match the BCrypt hash.
            log.warn("Login failed for email: {}", request.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // HTTP 401
        }
    }

    // ─── POST /api/auth/admin/login ──────────────────────────────────────
    // Admin logs in with email and password.
    // Admin credentials are stored in application.properties (not in the database).
    // @Valid → validates the AuthRequest fields (email format, password not blank).
    @PostMapping("/admin/login")
    public ResponseEntity<AuthResponse> adminLogin(@Valid @RequestBody AuthRequest request) {
        log.info("Admin login attempt for email: {}", request.getEmail());

        // Check: does the submitted email match the admin email AND does the submitted password
        // match the stored BCrypt hash?
        // passwordEncoder.matches(plainText, hashFromDB) → returns true if they match.
        // equalsIgnoreCase() → case-insensitive email comparison.
        if (adminEmail.equalsIgnoreCase(request.getEmail()) &&
                passwordEncoder.matches(request.getPassword(), adminPasswordHash)) {

            // Build an Authentication object representing the admin user.
            // UsernamePasswordAuthenticationToken(username, password, authorities):
            //   - username = admin email
            //   - password = null (we don't need it after verification)
            //   - authorities = [ROLE_ADMIN]
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    request.getEmail(), null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));

            // Generate a JWT token for the admin containing their email and ROLE_ADMIN.
            String token = jwtTokenProvider.generateToken(authentication);

            log.info("Admin login successful for email: {}", request.getEmail());
            // Return the JWT + email + roles in an AuthResponse DTO with HTTP 200 OK.
            return ResponseEntity.ok(new AuthResponse(token, request.getEmail(), Collections.singletonList("ROLE_ADMIN")));
        }

        // If credentials don't match → return HTTP 401 Unauthorized.
        log.warn("Invalid admin login credentials for email: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
