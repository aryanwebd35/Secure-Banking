package net.javaguides.Banking_app.entity;
// ↑ This file is in the "entity" package — entities represent database tables.

// ============================================================
// WHAT IS AccountStatus?
// This is an ENUM — a special type in Java that holds a FIXED list of allowed values.
// Instead of storing "active", "Active", "ACTIVE" (all inconsistent), we use an Enum
// to make sure only VALID values can ever be stored.
//
// The three possible states of a bank account:
//   ACTIVE  → Account is open and working normally
//   BLOCKED → Account is temporarily frozen (admin can unblock it)
//   CLOSED  → Account is permanently closed (no transactions allowed)
//
// In the database, this is stored as the text: "ACTIVE", "BLOCKED", or "CLOSED"
// This is because Account.java uses @Enumerated(EnumType.STRING)
// ============================================================
public enum AccountStatus {
    ACTIVE,   // ↑ Normal working state — deposits, withdrawals, and transfers all allowed
    BLOCKED,  // ↑ Temporarily frozen — no transactions allowed, but account still exists
    CLOSED    // ↑ Permanently closed — all operations blocked, typically never re-opened
}
