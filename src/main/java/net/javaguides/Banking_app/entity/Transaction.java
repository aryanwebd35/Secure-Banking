package net.javaguides.Banking_app.entity;
// ↑ This file is in the "entity" package — entities represent database tables.

import jakarta.persistence.*;
// ↑ Imports all JPA annotations (Entity, Table, Id, Column, Enumerated, etc.)

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
// ↑ Lombok annotations: auto-generate getters, setters, constructors.

import java.time.LocalDateTime;
// ↑ Java's date-time class. Stores both date and time, e.g., 2024-06-19T10:30:00.

// ============================================================
// WHAT IS Transaction?
// This class represents the "transactions" table in MySQL.
// Every deposit, withdrawal, or transfer creates a new row in this table.
//
// TABLE STRUCTURE:
//   transactions
//   ├── id                   (primary key, auto-incremented)
//   ├── transaction_type     (text: "DEPOSIT", "WITHDRAW", "TRANSFER")
//   ├── amount               (decimal number, e.g., 500.00)
//   ├── sender_account_id    (FK → accounts.id — who sent money, null for deposits)
//   ├── receiver_account_id  (FK → accounts.id — who received money, null for withdrawals)
//   ├── status               (text: "SUCCESS" or "FAILED")
//   ├── timestamp            (when the transaction happened)
//   ├── remarks              (optional note, e.g., "Rent payment")
//   ├── created_at           (inherited from BaseEntity)
//   └── updated_at           (inherited from BaseEntity)
// ============================================================

@Getter        // ↑ Lombok: auto-generates getId(), getAmount(), getStatus() etc.
@Setter        // ↑ Lombok: auto-generates setId(), setAmount(), setStatus() etc.
@NoArgsConstructor   // ↑ Lombok: generates Transaction() empty constructor (required by JPA)
@AllArgsConstructor  // ↑ Lombok: generates a constructor with ALL fields as parameters
@Entity              // ↑ JPA: this class represents a database table
@Table(name = "transactions") // ↑ JPA: the MySQL table name is "transactions"
public class Transaction extends BaseEntity {
    // ↑ Extends BaseEntity → inherits createdAt and updatedAt columns automatically.

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // ↑ Primary key that auto-increments (1, 2, 3, 4 ...)
    private Long id;

    // @Enumerated(EnumType.STRING) → Store as text ("DEPOSIT", "WITHDRAW", "TRANSFER") not as number.
    // @Column(name = "transaction_type", nullable = false) → Column name is "transaction_type",
    //   and it is required (cannot be null). Every transaction MUST have a type.
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;
    // TransactionType is an enum with values: DEPOSIT, WITHDRAW, TRANSFER (see TransactionType.java)

    @Column(nullable = false)
    // ↑ nullable = false → Every transaction must have an amount (cannot be null).
    private double amount;
    // The amount of money involved in this transaction. E.g., 500.0 (means 500 rupees).

    @Column(name = "sender_account_id")
    // ↑ Column name is "sender_account_id". This is null for DEPOSIT transactions
    //   (because no sender is involved when money is deposited externally).
    private Long senderAccountId;
    // The account ID of the person who SENT money.
    // Example: For a TRANSFER from Account #2 to Account #5, this would be 2.

    @Column(name = "receiver_account_id")
    // ↑ Column name is "receiver_account_id". This is null for WITHDRAW transactions.
    private Long receiverAccountId;
    // The account ID of the person who RECEIVED money.
    // Example: For a TRANSFER from Account #2 to Account #5, this would be 5.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    // ↑ Every transaction must have a status. Stored as "SUCCESS" or "FAILED".
    private TransactionStatus status;
    // TransactionStatus is an enum: SUCCESS or FAILED (see TransactionStatus.java)
    // Even FAILED transactions are saved to the database so we have a complete audit trail.

    @Column(nullable = false)
    // ↑ Every transaction must have a timestamp (cannot be null).
    private LocalDateTime timestamp;
    // The exact date and time when the transaction was executed.
    // Set in the service layer as: LocalDateTime.now()

    private String remarks;
    // An optional text note about this transaction.
    // Example: "Rent payment", "Birthday gift", "Utility bill"
    // This field CAN be null (no @Column(nullable = false)).
}
