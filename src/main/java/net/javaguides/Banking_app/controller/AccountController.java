package net.javaguides.Banking_app.controller; // Defines the package where this REST API controller class belongs

import java.util.List; // Imports standard Java Lists to return multiple records
import java.util.Map; // Imports Java Maps to read keys and values from request bodies

import org.springframework.http.HttpStatus; // Imports HTTP status helpers (like 200 OK, 201 Created)
import org.springframework.http.ResponseEntity; // Imports ResponseEntity to wrap the data and status code we send back to client
import org.springframework.security.core.Authentication; // Imports Spring Security Authentication to identify the logged-in user
import org.springframework.web.bind.annotation.DeleteMapping; // Imports annotation for HTTP DELETE request mapping
import org.springframework.web.bind.annotation.GetMapping; // Imports annotation for HTTP GET request mapping
import org.springframework.web.bind.annotation.PathVariable; // Imports annotation to read parameters from the URL path (e.g. /api/accounts/5)
import org.springframework.web.bind.annotation.PostMapping; // Imports annotation for HTTP POST request mapping
import org.springframework.web.bind.annotation.PutMapping; // Imports annotation for HTTP PUT request mapping
import org.springframework.web.bind.annotation.RequestBody; // Imports annotation to convert incoming JSON bodies into Java objects
import org.springframework.web.bind.annotation.RequestMapping; // Imports annotation to define base routes for all endpoints
import org.springframework.web.bind.annotation.RestController; // Imports RestController to mark this as a JSON API handler

import net.javaguides.Banking_app.dto.AccountDto; // Imports our Account DTO
import net.javaguides.Banking_app.service.AccountService; // Imports the service interface representing business rules
import lombok.extern.slf4j.Slf4j; // Imports Lombok helper to create a logger (used for print statement audits)
import jakarta.validation.Valid; // Imports validation triggers to check DTO validation constraints

// @RestController marks this class as an API Controller that returns JSON data back to the client.
@RestController 
// @RequestMapping sets the root url path for all endpoints in this class. All routes here start with "/api/accounts".
@RequestMapping("/api/accounts") 
// @Slf4j generates a `log` variable automatically, which allows us to print nice structured info/warning messages to console.
@Slf4j 
public class AccountController {

    private final AccountService accountService; // Reference to our service layer containing actual database operations

    // Constructor injection: Spring Boot will automatically inject the AccountService implementation when initializing this class.
    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    // Endpoint: GET /api/accounts/my
    // What it does: Fetches the account details of the currently logged-in customer.
    // Annotation @GetMapping mapping for HTTP GET requests
    @GetMapping("/my")
    public ResponseEntity<AccountDto> getMyAccount(Authentication authentication) {
        // authentication.getName() automatically extracts the user's email address from the validated JWT token!
        String email = authentication.getName(); 
        log.info("Fetching own account for user: {}", email); // Prints audit trace to console
        
        // Calls the service layer to locate the account for this email and returns it with a 200 OK HTTP status
        return ResponseEntity.ok(accountService.getMyAccount(email));
    }

    // Endpoint: PUT /api/accounts/my/deposit
    // What it does: Allows the logged-in customer to deposit money into their own account.
    // Annotation @PutMapping maps HTTP PUT requests (typically used for updates)
    @PutMapping("/my/deposit")
    public ResponseEntity<AccountDto> depositToMyAccount(Authentication authentication,
                                                          @RequestBody Map<String, Double> request) {
        String email = authentication.getName(); // Retrieve email from security token context
        AccountDto myAccount = accountService.getMyAccount(email); // Fetch this user's account info
        double amount = request.get("amount"); // Get the "amount" key from the input JSON body (e.g. {"amount": 500})
        log.info("Depositing {} to own account for user: {}", amount, email);
        
        // Execute deposit via account ID and return the updated account info
        return ResponseEntity.ok(accountService.deposit(myAccount.getId(), amount));
    }

    // Endpoint: PUT /api/accounts/my/withdraw
    // What it does: Allows the logged-in customer to withdraw money from their own account.
    @PutMapping("/my/withdraw")
    public ResponseEntity<AccountDto> withdrawFromMyAccount(Authentication authentication,
                                                             @RequestBody Map<String, Double> request) {
        String email = authentication.getName(); // Retrieve email from security token context
        AccountDto myAccount = accountService.getMyAccount(email); // Fetch user's account info
        double amount = request.get("amount"); // Extract "amount" from input body
        log.info("Withdrawing {} from own account for user: {}", amount, email);
        
        // Execute withdrawal and return updated account details
        return ResponseEntity.ok(accountService.withdraw(myAccount.getId(), amount));
    }

    // Endpoint: POST /api/accounts
    // What it does: Creates a brand new account (typically called by Admins or registration processes).
    // Annotation @PostMapping maps HTTP POST requests (typically used to create resources)
    @PostMapping
    public ResponseEntity<AccountDto> addAccount(@Valid @RequestBody AccountDto accountDto) {
        log.info("Creating account for holder: {}", accountDto.getAccountHolderName());
        AccountDto savedAccount = accountService.createAccount(accountDto); // Call service to insert into DB
        
        // Return the created DTO container along with HTTP Status 201 (CREATED)
        return new ResponseEntity<>(savedAccount, HttpStatus.CREATED);
    }

    // Endpoint: GET /api/accounts/{id} (e.g., /api/accounts/5)
    // What it does: Fetches a specific account details by its unique numeric ID.
    @GetMapping("/{id}")
    public ResponseEntity<AccountDto> getAccountById(@PathVariable Long id) {
        // @PathVariable maps the value in URL `{id}` directly to the `Long id` variable
        log.info("Fetching account ID: {}", id);
        AccountDto accountDto = accountService.getAccountById(id);
        return ResponseEntity.ok(accountDto);
    }

    // Endpoint: PUT /api/accounts/{id}/deposit
    // What it does: Depositing funds into a specific account by its numeric ID (Admin operation).
    @PutMapping("/{id}/deposit")
    public ResponseEntity<AccountDto> deposit(@PathVariable Long id, @RequestBody Map<String, Double> request) {
        double amount = request.get("amount");
        log.info("Depositing {} to account ID: {}", amount, id);
        AccountDto accountDto = accountService.deposit(id, amount);
        return ResponseEntity.ok(accountDto);
    }

    // Endpoint: PUT /api/accounts/{id}/withdraw
    // What it does: Withdrawing funds from a specific account by its numeric ID (Admin operation).
    @PutMapping("/{id}/withdraw")
    public ResponseEntity<AccountDto> withdraw(@PathVariable Long id, @RequestBody Map<String, Double> request) {
        double amount = request.get("amount");
        log.info("Withdrawing {} from account ID: {}", amount, id);
        AccountDto accountDto = accountService.withdraw(id, amount);
        return ResponseEntity.ok(accountDto);
    }

    // Endpoint: GET /api/accounts
    // What it does: Returns a list of all bank accounts in the database (Admin dashboard).
    @GetMapping
    public ResponseEntity<List<AccountDto>> getAllAccounts() {
        log.info("Fetching all accounts");
        List<AccountDto> accounts = accountService.getAllAccounts();
        return ResponseEntity.ok(accounts);
    }

    // Endpoint: DELETE /api/accounts/{id}
    // What it does: Deletes an account from the database (Admin dashboard).
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        log.info("Deleting account ID: {}", id);
        accountService.deleteAccount(id);
        
        // Return 204 No Content status indicating deletion succeeded and no response body is sent back
        return ResponseEntity.noContent().build();
    }
}

