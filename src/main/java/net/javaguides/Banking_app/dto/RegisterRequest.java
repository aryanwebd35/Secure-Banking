package net.javaguides.Banking_app.dto;
// ↑ Belongs to the "dto" (Data Transfer Object) package.

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

// ============================================================
// WHAT IS RegisterRequest?
// The request body DTO for the user registration endpoint.
//
// Used by:
//   POST /api/auth/register → New user registers an account
//
// Everything needed to create a new user + bank account is collected here
// in a single step (no 2-step Clerk onboarding flow needed).
//
// VALIDATION:
//   @NotBlank  → Field cannot be null/empty/whitespace
//   @Email     → Must be a valid email format
//   @Size      → Minimum length constraint on password
//   @Pattern   → Phone must be exactly 10 digits
// ============================================================

// @Data → Lombok: auto-generates getters, setters, toString, equals, hashCode.
@Data
public class RegisterRequest {

    // User's full name — displayed on the account and dashboard.
    @NotBlank(message = "Name is required")
    private String name;

    // User's email — used as the unique login identifier and for account lookup.
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    // User's chosen password — will be BCrypt-hashed before storing.
    // Minimum 6 characters for basic security.
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    // User's 10-digit phone number — used as the unique identifier for money transfers.
    // Example: "9876543210"
    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\d{10}$", message = "Phone number must be exactly 10 digits")
    private String phoneNumber;
}
