package net.javaguides.Banking_app.config;
// ↑ Belongs to the "config" package — configuration beans.

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

// ============================================================
// WHAT IS PasswordEncoderConfig?
// A standalone configuration class that ONLY provides the PasswordEncoder bean.
//
// WHY IS THIS A SEPARATE CLASS?
// To break a circular dependency that would happen if PasswordEncoder
// were defined inside SecurityConfig:
//
//   The cycle that would occur:
//     SecurityConfig → needs UserService (for DaoAuthenticationProvider)
//     UserService    → needs PasswordEncoder (for BCrypt hashing)
//     PasswordEncoder → defined IN SecurityConfig → back to SecurityConfig!
//
// Solution: put PasswordEncoder in its own config class with NO dependencies.
// Now the dependency graph is:
//   SecurityConfig → UserService → PasswordEncoderConfig (no cycle!)
//
// BCrypt is a strong, industry-standard password hashing algorithm.
// It is one-way (irreversible) and automatically adds a random "salt"
// to each hash, making it impossible to reverse-engineer the original password.
// Example: "password123" → "$2a$10$jDt/..." (always a different hash for same input)
// ============================================================

// @Configuration → This class defines Spring beans (configuration objects).
@Configuration
public class PasswordEncoderConfig {

    // @Bean → Spring manages the PasswordEncoder returned by this method.
    //   Injected into: UserServiceImpl (for hashing at registration)
    //                  SecurityConfig (for the DaoAuthenticationProvider — login verification)
    //                  AuthController (for admin password comparison)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
        // BCrypt automatically adds a "salt" (random data) to each hash,
        // making it impossible to reverse-engineer the original password.
        // Strength factor defaults to 10 (2^10 = 1024 rounds), which is secure and fast enough.
    }
}
