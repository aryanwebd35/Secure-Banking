package net.javaguides.Banking_app.mapper;
// ↑ Belongs to the "mapper" package.

import net.javaguides.Banking_app.dto.AccountDto;
// ↑ AccountDto = the data container for API requests and responses.

import net.javaguides.Banking_app.entity.Account;
// ↑ Account = the database entity (the "accounts" table row).

// ============================================================
// WHAT IS AccountMapper?
// A static utility class with two methods:
//   mapToAccount(AccountDto)    → Converts API DTO → Database Entity
//   mapToAccountDto(Account)    → Converts Database Entity → API DTO
//
// WHY DO WE NEED A MAPPER?
// The frontend speaks in DTOs (JSON data shapes).
// The database speaks in Entities (Java objects linked to tables).
// The mapper acts as a TRANSLATOR between the two worlds.
//
// ANALOGY: Like a bilingual interpreter.
// Frontend sends JSON → Mapper converts to Entity → Service saves to DB.
// DB returns Entity → Mapper converts to DTO → Controller sends JSON to frontend.
// ============================================================

// Why do we need a Mapper?
// It acts as a bridge. The frontend works with DTOs, but the database works with Entities.
// This class converts DTOs to Entities, and Entities back to DTOs.
public class AccountMapper {

    // ─── DTO → Entity ────────────────────────────────────────────────────
    // Converts an AccountDto (data sent from frontend) into an Account Entity (ready to save to MySQL)
    public static Account mapToAccount(AccountDto accountDto) {
        if (accountDto == null) {
            return null; // If the input data is blank/null, return null to avoid errors
        }

        Account account = new Account(); // Create a new blank database Entity object
        account.setId(accountDto.getId()); // Copy the unique ID (null for new accounts)
        account.setAccountHolderName(accountDto.getAccountHolderName()); // Copy the holder's name
        account.setBalance(accountDto.getBalance()); // Copy the balance amount

        // Convert the nested UserDto into a User Entity using the UserMapper.
        // AccountDto contains a UserDto → We need to convert it to a User entity for the Account.
        account.setUser(UserMapper.mapToUser(accountDto.getUser()));

        // Convert the String status (like "ACTIVE") from the DTO into the Database Enum type.
        // AccountStatus.valueOf("ACTIVE") → converts String to AccountStatus.ACTIVE enum value.
        if (accountDto.getAccountStatus() != null) {
            account.setAccountStatus(net.javaguides.Banking_app.entity.AccountStatus.valueOf(accountDto.getAccountStatus()));
        }

        return account; // Return the fully populated Entity object ready for database saving
    }

    // ─── Entity → DTO ────────────────────────────────────────────────────
    // Converts an Account Entity (data loaded from MySQL) into an AccountDto (ready to send back to the frontend)
    public static AccountDto mapToAccountDto(Account account) {
        if (account == null) {
            return null; // If database has no record (null), return null to avoid errors
        }

        AccountDto accountDto = new AccountDto(); // Create a new blank container DTO object
        accountDto.setId(account.getId()); // Copy the unique ID
        accountDto.setAccountHolderName(account.getAccountHolderName()); // Copy the holder's name
        accountDto.setBalance(account.getBalance()); // Copy the balance amount

        // Convert the nested User database entity into a UserDto using the UserMapper.
        // The frontend receives UserDto (not the full User entity) to avoid exposing sensitive fields.
        accountDto.setUser(UserMapper.mapToUserDto(account.getUser()));

        // Convert the Database Enum status into a plain String for the JSON response.
        // AccountStatus.ACTIVE → "ACTIVE" (string)
        // The frontend works with strings, not Java enum types.
        if (account.getAccountStatus() != null) {
            accountDto.setAccountStatus(account.getAccountStatus().name());
        }

        return accountDto; // Return the fully populated DTO object ready for JSON serialization
    }
}
