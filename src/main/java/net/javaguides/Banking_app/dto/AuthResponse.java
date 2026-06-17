package net.javaguides.Banking_app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private String email;
    private List<String> roles;
    private boolean needsPhoneSetup;

    // Convenience constructor for cases where phone setup is not needed
    public AuthResponse(String token, String email, List<String> roles) {
        this.token = token;
        this.email = email;
        this.roles = roles;
        this.needsPhoneSetup = false;
    }
}

