package net.javaguides.Banking_app.exception;

// ============================================================
// WHAT IS AccountNotFoundException?
// A CUSTOM EXCEPTION thrown when we try to find an Account in the DB but it doesn't exist.
//
// WHY CREATE CUSTOM EXCEPTIONS?
// Instead of using a generic "RuntimeException", custom exceptions give CLEAR names:
//   throw new AccountNotFoundException("Account not found for ID: 5")
// vs.
//   throw new RuntimeException("Account not found for ID: 5")
//
// Custom exceptions also allow GlobalExceptionHandler to catch SPECIFIC types and
// return the correct HTTP status code (e.g., 404 Not Found for missing accounts).
//
// extends RuntimeException:
//   RuntimeException = unchecked exception = you don't HAVE to use try/catch.
//   Methods that throw it don't need "throws AccountNotFoundException" in their signature.
//   This is the standard choice for business logic exceptions in Spring Boot.
// ============================================================
public class AccountNotFoundException extends RuntimeException {

    // Constructor: Create the exception with a message.
    // The "super(message)" call passes the message to the parent RuntimeException class.
    // This message is what appears in the JSON error response sent to the frontend.
    public AccountNotFoundException(String message) {
        super(message);
        // Example: new AccountNotFoundException("Account not found for ID: 5")
        // → exception.getMessage() returns "Account not found for ID: 5"
    }
}
