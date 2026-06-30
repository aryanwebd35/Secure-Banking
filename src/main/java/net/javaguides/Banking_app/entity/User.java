package net.javaguides.Banking_app.entity;
// ↑ This file belongs to the "entity" package — entities represent database tables.

import jakarta.persistence.*;
// ↑ Imports all JPA annotations at once (Entity, Table, Id, Column, etc.)

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
// ↑ Lombok annotations: auto-generate constructors and getter/setter methods.

import java.util.HashSet;
import java.util.Set;
// ↑ Set is like a List but does NOT allow duplicate values.
//   HashSet is one implementation of Set — stores items in no particular order.
//   We use a Set<Role> here because a user shouldn't have the same role twice.

// ============================================================
// WHAT IS User?
// This class represents the "users" table in MySQL.
// Each object = one row in the users table.
//
// TABLE STRUCTURE:
//   users
//   ├── id               (primary key, auto-incremented)
//   ├── name             (full name of user, NOT NULL)
//   ├── email            (unique, not null — used as login identifier)
//   ├── phone_number     (unique — used for money transfers between users)
//   ├── password         (BCrypt hash — required for ALL users now, not just admin)
//   ├── created_at       (inherited from BaseEntity)
//   └── updated_at       (inherited from BaseEntity)
//
// There is also a JOIN TABLE "user_roles" created automatically by JPA:
//   user_roles
//   ├── user_id   (FK → users.id)
//   └── role_id   (FK → roles.id)
// ============================================================

@Getter           // ↑ Lombok: auto-generates getId(), getName(), getEmail() etc.
@Setter           // ↑ Lombok: auto-generates setId(), setName(), setEmail() etc.
@NoArgsConstructor   // ↑ Lombok: generates User() empty constructor (required by JPA)
@AllArgsConstructor  // ↑ Lombok: generates User(id, name, email, ...) full constructor
@Entity              // ↑ JPA: this class maps to a database table
@Table(name = "users") // ↑ JPA: the table is named "users" in MySQL
public class User extends BaseEntity {
    // ↑ "extends BaseEntity" → User table also gets createdAt, updatedAt columns automatically.

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // ↑ @Id = primary key. @GeneratedValue = auto-increment (1, 2, 3, 4...)
    private Long id;

    @Column(nullable = false)
    // ↑ nullable = false → name CANNOT be null. Every user must have a name.
    private String name;
    // Full name of the user, e.g., "Aryan Sharma"

    @Column(nullable = false, unique = true)
    // ↑ nullable = false → email is required. unique = true → no two users share the same email.
    //   Email is used as the login identifier (the "username" in JWT token).
    private String email;

    @Column(name = "phone_number", unique = true)
    // ↑ unique = true → each phone number belongs to only one account.
    //   Phone numbers are the identifier used for peer-to-peer money transfers.
    private String phoneNumber;

    @Column(nullable = false)
    // ↑ nullable = false → password is required for all users.
    //   The value stored here is ALWAYS a BCrypt HASH — NEVER plain text!
    //   BCrypt example: "password123" → "$2a$10$voRCWJN8Vuk..."
    private String password;

    // @ManyToMany → One User can have MANY Roles, and one Role can be assigned to MANY Users.
    //   Example: "ROLE_USER" role is shared by all regular users.
    // fetch = FetchType.EAGER → Load the roles immediately when loading the User object.
    // cascade = CascadeType.ALL → If a User is saved/deleted, also save/delete their roles.
    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    // @JoinTable creates a JOIN TABLE in MySQL named "user_roles":
    //   user_roles
    //   ├── user_id   → references users.id
    //   └── role_id   → references roles.id
    @JoinTable(
            name = "user_roles",                                          // Name of the join table
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),        // This side (User)
            inverseJoinColumns = @JoinColumn(name = "role_id", referencedColumnName = "id")  // Other side (Role)
    )
    private Set<Role> roles = new HashSet<>();
    // A Set of Role objects. Starts as an empty HashSet.
    // Example: roles = {Role(id=1, name="ROLE_USER")}
}
