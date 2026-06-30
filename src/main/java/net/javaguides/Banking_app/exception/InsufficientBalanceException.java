package net.javaguides.Banking_app.exception;

// ============================================================
// THROWN WHEN: A user tries to withdraw or transfer more money than their account balance.
// HANDLED BY: GlobalExceptionHandler → returns HTTP 400 Bad Request
// EXAMPLE: "Insufficient balance in account ID: 3"
// ============================================================
public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}
