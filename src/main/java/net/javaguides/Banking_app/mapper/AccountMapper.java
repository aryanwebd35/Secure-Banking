package net.javaguides.Banking_app.mapper; // Defines the package where this Mapper class belongs

import net.javaguides.Banking_app.dto.AccountDto; // Imports our temporary container DTO class
import net.javaguides.Banking_app.entity.Account; // Imports our MySQL database table entity class

// Why do we need a Mapper?
// It acts as a bridge. The frontend works with DTOs, but the database works with Entities.
// This class converts DTOs to Entities, and Entities back to DTOs.
public class AccountMapper {

    // Converts an AccountDto (data sent from frontend) into an Account Entity (ready to save to MySQL)
    public static Account mapToAccount(AccountDto accountDto) {
        if (accountDto == null) {
            return null; // If the input data is blank/null, return null to avoid errors
        }
        
        Account account = new Account(); // Create a new blank database Entity object
        account.setId(accountDto.getId()); // Copy the unique ID
        account.setAccountHolderName(accountDto.getAccountHolderName()); // Copy the holder's name
        account.setBalance(accountDto.getBalance()); // Copy the balance amount
        
        // Convert the nested UserDto into a User Entity using the UserMapper
        account.setUser(UserMapper.mapToUser(accountDto.getUser())); 
        
        // Convert the String status (like "ACTIVE") from the DTO into the Database Enum type
        if (accountDto.getAccountStatus() != null) {
            account.setAccountStatus(net.javaguides.Banking_app.entity.AccountStatus.valueOf(accountDto.getAccountStatus()));
        }
        
        return account; // Return the fully populated Entity object
    }

    // Converts an Account Entity (data loaded from MySQL) into an AccountDto (ready to send back to the frontend)
    public static AccountDto mapToAccountDto(Account account) {
        if (account == null) {
            return null; // If database has no record (null), return null to avoid errors
        }
        
        AccountDto accountDto = new AccountDto(); // Create a new blank container DTO object
        accountDto.setId(account.getId()); // Copy the unique ID
        accountDto.setAccountHolderName(account.getAccountHolderName()); // Copy the holder's name
        accountDto.setBalance(account.getBalance()); // Copy the balance amount
        
        // Convert the nested User database entity into a UserDto using the UserMapper
        accountDto.setUser(UserMapper.mapToUserDto(account.getUser()));
        
        // Convert the Database Enum status into a plain String (e.g. "ACTIVE") for the JSON response
        if (account.getAccountStatus() != null) {
            accountDto.setAccountStatus(account.getAccountStatus().name());
        }
        
        return accountDto; // Return the fully populated DTO object
    }
}

