package net.javaguides.Banking_app.dto;
// ↑ Belongs to the "dto" (Data Transfer Object) package.
//   DTOs carry data between the HTTP layer and the service layer.

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// ============================================================
// WHAT IS AuthRequest?
// The request body DTO for the login endpoint.
//
// Used by:
//   POST /api/auth/login       → User login (email + password)
//   POST /api/auth/admin/login → Admin login (email + password)
//
// VALIDATION:
//   @Email       → Spring validates that the email has a proper format (x@y.z)
//   @NotBlank    → Field cannot be null, empty, or just whitespace
// ============================================================

// @Data → Lombok: auto-generates getters, setters, toString, equals, hashCode.
@Data
public class AuthRequest {

    // The user's email address — used as the unique login identifier.
    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    private String email;

    // The user's plain-text password — compared against the BCrypt hash stored in the DB.
    // NEVER stored — only used to verify against the stored hash at login time.
    @NotBlank(message = "Password is required")
    private String password;
}
