package net.javaguides.Banking_app.entity;
// ↑ This file is in the "entity" package.

// ============================================================
// WHAT IS TransactionType?
// An ENUM that defines the three types of transactions this banking app supports.
//
// Why use an Enum? → To make sure only valid values are ever used.
//   BAD:  transaction.setType("DEPOSITT") ← typo, bad value, no error at compile time!
//   GOOD: transaction.setType(TransactionType.DEPOSIT) ← compiler will catch any typos!
// ============================================================
public enum TransactionType {
    DEPOSIT,   // ↑ Money added to an account from outside (e.g., cash deposit at bank)
    WITHDRAW,  // ↑ Money taken OUT of an account (e.g., ATM withdrawal)
    TRANSFER   // ↑ Money moved from one account to another within the same bank
}
