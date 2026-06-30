package net.javaguides.Banking_app.exception;
// ↑ Belongs to the "exception" package — custom exception classes.

import net.javaguides.Banking_app.dto.ErrorDetails;
// ↑ ErrorDetails is a DTO that holds error info to send back to the client.
//   Contains: timestamp, message, and request path.

import org.springframework.http.HttpStatus;
// ↑ HttpStatus holds standard HTTP status codes:
//   HttpStatus.NOT_FOUND = 404, HttpStatus.BAD_REQUEST = 400, HttpStatus.UNAUTHORIZED = 401 etc.

import org.springframework.http.ResponseEntity;
// ↑ ResponseEntity wraps the response body AND the HTTP status code.

import org.springframework.web.bind.MethodArgumentNotValidException;
// ↑ This exception is thrown by Spring when @Valid validation fails on a request body.
//   Example: user sends an email with wrong format → this exception is thrown.

import org.springframework.web.bind.annotation.ControllerAdvice;
// ↑ @ControllerAdvice is a GLOBAL exception handler.
//   It intercepts exceptions thrown by ANY controller in the entire app.
//   Without it, exceptions would result in ugly, confusing Spring error pages.

import org.springframework.web.bind.annotation.ExceptionHandler;
// ↑ @ExceptionHandler marks a method that handles a SPECIFIC type of exception.
//   Example: @ExceptionHandler(AccountNotFoundException.class) → handles AccountNotFoundException.

import org.springframework.web.context.request.WebRequest;
// ↑ WebRequest gives us info about the current HTTP request (like the URL path).

import java.time.LocalDateTime;
import java.util.stream.Collectors;

// ============================================================
// WHAT IS GlobalExceptionHandler?
// A centralized place to handle ALL exceptions that happen in the application.
//
// WHY DO WE NEED THIS?
// Without this class, when an exception is thrown:
//   - The user sees an ugly 500 Internal Server Error or a Java stack trace.
//   - The response doesn't follow a consistent format.
//
// WITH this class:
//   - Every exception returns a clean, consistent JSON response.
//   - The appropriate HTTP status code is always used.
//   - Example response:
//     {
//       "timestamp": "2024-06-19T10:30:00",
//       "message": "Account not found for ID: 5",
//       "details": "uri=/api/accounts/5"
//     }
//
// HOW IT WORKS:
// @ControllerAdvice → Spring wraps this class around all controllers.
// @ExceptionHandler(XException.class) → when XException is thrown ANYWHERE,
//   Spring calls the corresponding method here instead of crashing.
// ============================================================

// @ControllerAdvice → This class applies globally to all @RestController and @Controller classes.
//   Think of it as a "catch-all" layer that sits around all controllers.
@ControllerAdvice
public class GlobalExceptionHandler {

    // ─── Handler 1: User not found ───────────────────────────────────────
    // @ExceptionHandler(UserNotFoundException.class) → triggered when UserNotFoundException is thrown.
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorDetails> handleUserNotFoundException(UserNotFoundException exception, WebRequest webRequest) {
        // Create an ErrorDetails object with: current time, error message, and URL path.
        ErrorDetails errorDetails = new ErrorDetails(
                LocalDateTime.now(),                    // When the error happened
                exception.getMessage(),                 // The error message (e.g., "User not found for email: x")
                webRequest.getDescription(false)        // The request path (e.g., "uri=/api/accounts/my")
        );
        // Return the ErrorDetails with HTTP 404 Not Found status.
        return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND);
    }

    // ─── Handler 2: Account not found ────────────────────────────────────
    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorDetails> handleAccountNotFoundException(AccountNotFoundException exception, WebRequest webRequest) {
        ErrorDetails errorDetails = new ErrorDetails(LocalDateTime.now(), exception.getMessage(), webRequest.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.NOT_FOUND); // HTTP 404
    }

    // ─── Handler 3: Insufficient balance ─────────────────────────────────
    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorDetails> handleInsufficientBalanceException(InsufficientBalanceException exception, WebRequest webRequest) {
        ErrorDetails errorDetails = new ErrorDetails(LocalDateTime.now(), exception.getMessage(), webRequest.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST); // HTTP 400 - client's fault (not enough money)
    }

    // ─── Handler 4: Unauthorized access ──────────────────────────────────
    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ErrorDetails> handleUnauthorizedAccessException(UnauthorizedAccessException exception, WebRequest webRequest) {
        ErrorDetails errorDetails = new ErrorDetails(LocalDateTime.now(), exception.getMessage(), webRequest.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.UNAUTHORIZED); // HTTP 401
    }

    // ─── Handler 5: Account blocked or closed ────────────────────────────
    @ExceptionHandler(AccountBlockedException.class)
    public ResponseEntity<ErrorDetails> handleAccountBlockedException(AccountBlockedException exception, WebRequest webRequest) {
        ErrorDetails errorDetails = new ErrorDetails(LocalDateTime.now(), exception.getMessage(), webRequest.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.FORBIDDEN); // HTTP 403 - forbidden
    }

    // ─── Handler 6: Invalid transaction ──────────────────────────────────
    @ExceptionHandler(InvalidTransactionException.class)
    public ResponseEntity<ErrorDetails> handleInvalidTransactionException(InvalidTransactionException exception, WebRequest webRequest) {
        ErrorDetails errorDetails = new ErrorDetails(LocalDateTime.now(), exception.getMessage(), webRequest.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST); // HTTP 400
    }

    // ─── Handler 7: Validation errors (@Valid) ────────────────────────────
    // Thrown when request body fields fail @NotBlank, @Email, @Min etc. validation rules.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDetails> handleValidationException(MethodArgumentNotValidException exception, WebRequest webRequest) {
        // Collect all field validation errors into one readable string.
        // exception.getBindingResult().getFieldErrors() → list of individual field errors.
        // .stream().map(error -> "fieldName: message") → format each error.
        // .collect(Collectors.joining(", ")) → join them with ", " separator.
        // Example: "email: must be a valid email address, amount: must be greater than 0"
        String errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ErrorDetails errorDetails = new ErrorDetails(LocalDateTime.now(), "Validation Failed", errors);
        return new ResponseEntity<>(errorDetails, HttpStatus.BAD_REQUEST); // HTTP 400
    }

    // ─── Handler 8: Catch-all for unexpected errors ───────────────────────
    // This catches ANY exception that isn't handled by the specific handlers above.
    // This is a "safety net" — even unexpected exceptions return a clean JSON response.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDetails> handleGlobalException(Exception exception, WebRequest webRequest) {
        ErrorDetails errorDetails = new ErrorDetails(LocalDateTime.now(), exception.getMessage(), webRequest.getDescription(false));
        return new ResponseEntity<>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR); // HTTP 500
    }
}
