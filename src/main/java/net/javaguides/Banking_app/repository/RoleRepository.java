package net.javaguides.Banking_app.repository;
// ↑ Belongs to the "repository" package — database access layer.

import net.javaguides.Banking_app.entity.Role;
// ↑ Imports the Role entity so this repository works with the "roles" table.

import org.springframework.data.jpa.repository.JpaRepository;
// ↑ Gives us FREE database methods (save, findById, findAll, delete, count, etc.)

import java.util.Optional;
// ↑ Optional<T> — safe container that may or may not hold a Role value.

// ============================================================
// WHAT IS RoleRepository?
// Database access layer for the "roles" table.
// Roles are simple: just an id and a name ("ROLE_USER", "ROLE_ADMIN").
//
// HOW IT'S USED:
//   When a new user registers, we check if "ROLE_USER" already exists in the DB.
//   If it does, we reuse it. If not, we create it.
//   See: UserServiceImpl.registerOrUpdateUser()
//
//   Code example:
//   Role userRole = roleRepository.findByName("ROLE_USER")
//       .orElseGet(() -> roleRepository.save(new Role(null, "ROLE_USER")));
//   → Find the ROLE_USER role, or create it if it doesn't exist yet.
// ============================================================
public interface RoleRepository extends JpaRepository<Role, Long> {
    // ↑ Works with Role entities and Long primary key.

    // Auto-generated SQL: SELECT * FROM roles WHERE name = ?
    Optional<Role> findByName(String name);
    // Used for: Looking up a role by its name (e.g., "ROLE_USER" or "ROLE_ADMIN").
    //           Returns Optional because the role might not exist yet in the database.
}
