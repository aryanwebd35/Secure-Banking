package net.javaguides.Banking_app.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthRequest {

    // Admin login fields
    @Email(message = "Invalid email format")
    private String email;
    private String password;

    // Clerk login token
    private String clerkToken;

    // Customer simulated login name
    private String name;
}
