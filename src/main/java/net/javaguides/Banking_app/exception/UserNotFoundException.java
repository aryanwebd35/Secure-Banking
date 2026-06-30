package net.javaguides.Banking_app.exception;

// ============================================================
// THROWN WHEN: We try to find a User in the database but they don't exist.
// HANDLED BY: GlobalExceptionHandler → returns HTTP 404 Not Found
// EXAMPLE: "User not found for email: unknown@gmail.com"
// ============================================================
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
