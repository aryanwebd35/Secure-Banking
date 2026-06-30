package net.javaguides.Banking_app.entity;
// ↑ Belongs to the "entity" package (database table representations).

import jakarta.persistence.*;
// ↑ Imports all JPA annotations at once.

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
// ↑ Lombok: auto-generate getters, setters, and constructors.

// ============================================================
// WHAT IS Role?
// This class represents the "roles" table in MySQL.
// A Role is simply a label that describes WHAT a user is allowed to do.
//
// In this app, there are two roles:
//   - "ROLE_USER"  → Regular banking customer
//   - "ROLE_ADMIN" → Bank administrator
//
// Spring Security uses these role names to decide WHAT each user is ALLOWED to access.
// For example, only ROLE_ADMIN can access /admin/** endpoints.
//
// TABLE STRUCTURE:
//   roles
//   ├── id    (primary key, auto-incremented)
//   └── name  (unique, e.g., "ROLE_USER", "ROLE_ADMIN")
// ============================================================

@Getter        // ↑ Lombok: generates getId() and getName()
@Setter        // ↑ Lombok: generates setId() and setName()
@NoArgsConstructor   // ↑ Lombok: generates Role() empty constructor (required by JPA)
@AllArgsConstructor  // ↑ Lombok: generates Role(id, name) constructor
@Entity              // ↑ JPA: this class = a database table
@Table(name = "roles") // ↑ JPA: the MySQL table name is "roles"
public class Role {
    // Note: Role does NOT extend BaseEntity because we don't need createdAt/updatedAt for roles.
    // Roles are static and rarely change.

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // ↑ Primary key that auto-increments. (ROLE_USER = 1, ROLE_ADMIN = 2)
    private Long id;

    @Column(unique = true, nullable = false)
    // ↑ unique = true → No two roles can have the same name.
    //   nullable = false → Every role MUST have a name.
    private String name;
    // The role name. Spring Security requires the prefix "ROLE_" (e.g., "ROLE_USER", "ROLE_ADMIN").
    // This is just a convention — Spring Security checks for roles using the "ROLE_" prefix.
}
