package net.javaguides.Banking_app.entity;
// ↑ This file is inside the "entity" package — entities represent database tables.

import jakarta.persistence.Column;
// ↑ @Column lets us customize how this field is stored in the database.

import jakarta.persistence.Entity;
// ↑ @Entity tells Spring/JPA: "This Java class represents a TABLE in MySQL."
//   JPA (Java Persistence API) is the standard way Java apps talk to databases.

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
// ↑ @Enumerated(EnumType.STRING) tells JPA: "Store the Enum value as a TEXT string in the DB."
//   For example, AccountStatus.ACTIVE is stored in MySQL as the string "ACTIVE", not as a number.

import jakarta.persistence.FetchType;
// ↑ FetchType decides WHEN to load related data from the database:
//   - FetchType.EAGER  = Load immediately (when you load Account, also load its User right away)
//   - FetchType.LAZY   = Load only when you ask for it (saves database queries)

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
// ↑ @GeneratedValue(strategy = GenerationType.IDENTITY) tells the database:
//   "Auto-increment the ID number for each new row (1, 2, 3, 4...)"
//   MySQL does this automatically when strategy = IDENTITY.

import jakarta.persistence.Id;
// ↑ @Id marks a field as the PRIMARY KEY of the database table.
//   Every table must have one primary key to uniquely identify each row.

import jakarta.persistence.JoinColumn;
// ↑ @JoinColumn defines the foreign key column that links this table to another table.
//   It's used with @OneToOne / @OneToMany / @ManyToOne.

import jakarta.persistence.OneToOne;
// ↑ @OneToOne means: "One Account belongs to exactly One User, and one User has only one Account."

import jakarta.persistence.Table;
// ↑ @Table(name = "accounts") tells JPA: "This entity maps to a MySQL table named 'accounts'."
//   If you don't specify, JPA uses the class name ("Account") as the table name.

import lombok.AllArgsConstructor;
// ↑ @AllArgsConstructor → Lombok generates a constructor with ALL fields as parameters.
//   e.g., new Account(id, accountHolderName, balance, user, accountStatus)

import lombok.Getter;
// ↑ @Getter → Lombok generates getId(), getAccountHolderName(), getBalance() etc.

import lombok.NoArgsConstructor;
// ↑ @NoArgsConstructor → Lombok generates a constructor with NO parameters.
//   e.g., new Account() — needed by JPA to create objects when loading from DB.

import lombok.Setter;
// ↑ @Setter → Lombok generates setId(), setAccountHolderName(), setBalance() etc.

// ============================================================
// WHAT IS Account?
// This class represents the "accounts" table in MySQL.
// Each object of this class = one row in the accounts table.
//
// TABLE STRUCTURE created by this entity:
//   accounts
//   ├── id               (primary key, auto-incremented)
//   ├── account_holder_name
//   ├── balance
//   ├── user_id          (foreign key → users.id)
//   ├── account_status   (text: "ACTIVE", "BLOCKED", "CLOSED")
//   ├── created_at       (inherited from BaseEntity)
//   └── updated_at       (inherited from BaseEntity)
// ============================================================

// @Getter / @Setter → Lombok auto-generates all getter/setter methods
@Getter
@Setter
// @NoArgsConstructor → Generates Account() constructor (no parameters) — needed by JPA
@NoArgsConstructor
// @AllArgsConstructor → Generates Account(id, name, balance, user, status) constructor
@AllArgsConstructor
// @Table(name = "accounts") → This entity maps to the MySQL table named "accounts"
@Table(name = "accounts")
// @Entity → This class is a JPA entity (= database table representation)
@Entity
public class Account extends BaseEntity {
    // ↑ "extends BaseEntity" means Account INHERITS createdAt and updatedAt fields from BaseEntity.
    //   These two fields automatically get added to the "accounts" MySQL table too.

    // @Id → This field is the PRIMARY KEY of the "accounts" table.
    // @GeneratedValue(strategy = GenerationType.IDENTITY) → MySQL auto-increments this (1, 2, 3, ...)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // "Long" is a 64-bit number type. We use Long (not int) for IDs because tables can grow very large.

    // @Column(name = "account_holder_name") → Store this field in a column called "account_holder_name"
    //   Without this annotation, JPA would use "accountHolderName" as the column name.
    @Column(name = "account_holder_name")
    private String accountHolderName;
    // This is the full name of the person who owns this account (e.g., "Aryan Sharma")

    private double balance;
    // The current money balance in the account. "double" is a decimal number type (e.g., 1500.75)

    // @OneToOne(fetch = FetchType.EAGER) → 
    //   One Account is linked to exactly One User.
    //   FetchType.EAGER = When we load an Account, Spring also IMMEDIATELY loads the linked User from DB.
    // @JoinColumn(name = "user_id", referencedColumnName = "id") →
    //   The "accounts" table has a column called "user_id" which is a foreign key pointing to "users.id"
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;
    // This is a reference to the User who owns this account.
    // IMPORTANT: The "user_id" column in accounts table stores the User's primary key.

    // @Enumerated(EnumType.STRING) → Store enum value as TEXT in the database.
    //   AccountStatus.ACTIVE  → "ACTIVE" in MySQL
    //   AccountStatus.BLOCKED → "BLOCKED" in MySQL
    // @Column(name = "account_status", nullable = false) →
    //   Column name is "account_status" and it can NEVER be null (always must have a value).
    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false)
    private AccountStatus accountStatus = AccountStatus.ACTIVE;
    // Default value is ACTIVE — so every new account starts as ACTIVE automatically.
}
