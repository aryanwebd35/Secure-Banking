package net.javaguides.Banking_app.exception;

// ============================================================
// THROWN WHEN: A user tries to do a transaction on a BLOCKED or CLOSED account.
// HANDLED BY: GlobalExceptionHandler → returns HTTP 403 Forbidden
// EXAMPLE: "Account is BLOCKED. Cannot perform transfer out"
// ============================================================
public class AccountBlockedException extends RuntimeException {
    // Passes the error message up to RuntimeException.
    public AccountBlockedException(String message) {
        super(message);
    }
}
