package net.javaguides.Banking_app.exception;

// ============================================================
// THROWN WHEN: A user tries to access a resource they are not authorized to use.
// HANDLED BY: GlobalExceptionHandler → returns HTTP 401 Unauthorized
// EXAMPLE: A regular user tries to perform an admin action.
// ============================================================
public class UnauthorizedAccessException extends RuntimeException {
    public UnauthorizedAccessException(String message) {
        super(message);
    }
}
