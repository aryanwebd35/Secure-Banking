package net.javaguides.Banking_app.dto;
// ↑ Belongs to the "dto" (Data Transfer Object) package.

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// ============================================================
// WHAT IS AuthResponse?
// The JSON response body returned after a successful login or registration.
//
// Returned by:
//   POST /api/auth/register      → After successful registration
//   POST /api/auth/login         → After successful login
//   POST /api/auth/admin/login   → After successful admin login
//
// FIELDS:
//   token  → The JWT token. Frontend stores this and sends it in every future request
//            as: "Authorization: Bearer <token>"
//   email  → The authenticated user's email (for display purposes)
//   roles  → List of roles, e.g., ["ROLE_USER"] or ["ROLE_ADMIN"]
// ============================================================

// @Data → Lombok: auto-generates getters, setters, toString, equals, hashCode.
// @AllArgsConstructor → Lombok: generates constructor with all fields.
// @NoArgsConstructor  → Lombok: generates empty constructor (needed for JSON deserialization).
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {

    // The signed JWT token to be used in the Authorization header for all future requests.
    // Format when sent in requests: "Authorization: Bearer eyJhbGci..."
    private String token;

    // The email of the authenticated user (useful for the frontend to display who is logged in).
    private String email;

    // The list of roles granted to this user, e.g., ["ROLE_USER"] or ["ROLE_ADMIN"].
    // The frontend uses this to decide which UI elements to show/hide.
    private List<String> roles;
}
