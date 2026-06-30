package net.javaguides.Banking_app.exception;

// ============================================================
// THROWN WHEN: A transaction is invalid in any way:
//   - Deposit/withdrawal amount is 0 or negative
//   - Transfer to the same account (self-transfer)
//   - Transaction ID doesn't exist when looking up
// HANDLED BY: GlobalExceptionHandler → returns HTTP 400 Bad Request
// EXAMPLE: "Deposit amount must be greater than zero"
// ============================================================
public class InvalidTransactionException extends RuntimeException {
    public InvalidTransactionException(String message) {
        super(message);
    }
}
