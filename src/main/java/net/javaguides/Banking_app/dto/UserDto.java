package net.javaguides.Banking_app.dto;
// ↑ Belongs to the "dto" (Data Transfer Object) package.

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// ============================================================
// WHAT IS UserDto?
// A Data Transfer Object representing user information sent to/from the API.
// It is a "safe" view of the User entity — it never exposes the password field.
//
// Used by:
//   - Admin endpoints that list or view user info
//   - Registration/login responses when user info is returned
//   - UserService return types
//
// DIFFERENCE FROM User.java (entity):
//   User.java   = directly maps to the "users" database table (has password, roles set, etc.)
//   UserDto.java = a lightweight, safe response object for the API layer
// ============================================================

// @Data → Lombok: getters, setters, toString, equals, hashCode.
// @NoArgsConstructor → Empty constructor (needed for JSON deserialization).
// @AllArgsConstructor → Full constructor for manual creation.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    // The database auto-generated primary key for this user.
    private Long id;

    // User's full name, e.g., "Aryan Sharma"
    @NotBlank(message = "Name is required")
    private String name;

    // User's email — the unique login identifier.
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    // User's 10-digit phone number — used for money transfers between users.
    // Not validated here (it was already validated at registration via RegisterRequest).
    private String phoneNumber;

    // Audit timestamps — automatically set by BaseEntity using JPA lifecycle callbacks.
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
