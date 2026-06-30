package net.javaguides.Banking_app.mapper;
// ↑ Belongs to the "mapper" package — converts between Entity (DB) ↔ DTO (API).

import net.javaguides.Banking_app.dto.UserDto;
// ↑ UserDto is the data container used in API requests/responses.

import net.javaguides.Banking_app.entity.User;
// ↑ User is the database entity (the "users" table row).

// ============================================================
// WHAT IS UserMapper?
// A static utility class that converts between User entity and UserDto.
//
// TWO DIRECTIONS:
//   Entity → DTO (mapToUserDto): When sending user data to the frontend.
//   DTO → Entity (mapToUser):    When creating a User from incoming DTO data.
//
// IMPORTANT: We deliberately EXCLUDE the "password" and "roles" fields from the DTO
// to avoid leaking sensitive security data in API responses.
// The frontend should NEVER see password hashes or role details.
// ============================================================
public class UserMapper {

    // ─── Entity → DTO ────────────────────────────────────────────────────
    // Converts a User database entity → UserDto (safe to send in API response).
    // "Safe" means no password hash, no roles — only data the frontend needs.
    public static UserDto mapToUserDto(User user) {
        if (user == null) {
            return null; // Null-safety guard
        }

        UserDto dto = new UserDto(); // Create a blank DTO object

        dto.setId(user.getId());                    // Copy the user's database ID
        dto.setName(user.getName());                // Copy the user's full name
        dto.setEmail(user.getEmail());              // Copy the email address
        dto.setPhoneNumber(user.getPhoneNumber());  // Copy the phone number
        dto.setCreatedAt(user.getCreatedAt());      // Copy creation timestamp (from BaseEntity)
        dto.setUpdatedAt(user.getUpdatedAt());      // Copy last update timestamp (from BaseEntity)

        // NOTICE: We do NOT include password or roles in the DTO!
        // Password hash should NEVER be sent to the frontend (security risk).
        // Role info is already embedded in the JWT token — no need to repeat it here.

        return dto;
    }

    // ─── DTO → Entity ────────────────────────────────────────────────────
    // Converts a UserDto → User entity (for saving to the database).
    // NOTE: In the current flow, registration is handled directly in UserServiceImpl
    // and does NOT go through this mapper (because we need to hash the password there).
    // This method is kept for potential admin use cases or future extensions.
    public static User mapToUser(UserDto dto) {
        if (dto == null) {
            return null; // Null-safety guard
        }

        User user = new User(); // Create a blank User entity

        user.setId(dto.getId());                    // Copy the ID (null for new users, set for updates)
        user.setName(dto.getName());                // Copy the name
        user.setEmail(dto.getEmail());              // Copy the email
        user.setPhoneNumber(dto.getPhoneNumber());  // Copy the phone number

        // NOTICE: We do NOT set password or roles from DTO.
        // Password is always hashed in the service layer (never set from DTO directly).
        // Roles are managed separately in the service layer using RoleRepository.

        return user;
    }
}
