package net.javaguides.Banking_app.dto; // Defines the package where this Data Transfer Object belongs

import lombok.AllArgsConstructor; // Imports Lombok helper to create a constructor with all arguments
import lombok.Data; // Imports Lombok helper that auto-generates getters, setters, toString, equals, and hashCode methods
import lombok.NoArgsConstructor; // Imports Lombok helper to create an empty (no-argument) constructor

// What is a DTO (Data Transfer Object)? 
// It is a temporary container class used to pass data between the frontend and the backend REST API.
// We use DTOs instead of raw database Entities to avoid exposing sensitive database table details to the internet.

@Data // Generates getters (e.g. getId, getBalance) and setters (e.g. setId, setBalance) automatically so we don't have to write them!
@AllArgsConstructor // Generates a constructor that accepts values for all variables below (e.g., new AccountDto(id, name, balance, user, status))
@NoArgsConstructor // Generates an empty default constructor (e.g., new AccountDto()) which is required by Spring
public class AccountDto {
    
    private Long id; // The unique ID number of the bank account (e.g., 1, 2, 3...)
    
    private String accountHolderName; // The name of the customer who owns this account
    
    private double balance; // The amount of money in the account (e.g., 1000.50)
    
    private UserDto user; // The user profile details linked to this account (embedded DTO inside a DTO)
    
    private String accountStatus; // The status of this account represented as a string (e.g. "ACTIVE", "BLOCKED")
}

