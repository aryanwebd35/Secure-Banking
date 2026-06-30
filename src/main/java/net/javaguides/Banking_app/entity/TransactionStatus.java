package net.javaguides.Banking_app.entity;
// ↑ This file is in the "entity" package.

// ============================================================
// WHAT IS TransactionStatus?
// An ENUM that tracks whether a transaction was successful or not.
//
// Why do we save FAILED transactions?
// → For audit trail / history purposes.
//   If a user tries to transfer ₹5000 but has only ₹2000 balance,
//   we save a FAILED transaction record so both the bank and user
//   can see what was attempted. This is also important for debugging.
// ============================================================
public enum TransactionStatus {
    SUCCESS, // ↑ The transaction completed successfully (money actually moved)
    FAILED   // ↑ The transaction failed (e.g., insufficient balance, blocked account)
             //   A record is still saved to the database for audit purposes.
}
