package net.javaguides.Banking_app.controller;

import io.jsonwebtoken.Claims;
import net.javaguides.Banking_app.dto.AuthRequest;
import net.javaguides.Banking_app.dto.AuthResponse;
import net.javaguides.Banking_app.dto.PhoneSetupRequest;
import net.javaguides.Banking_app.dto.UserDto;
import net.javaguides.Banking_app.security.ClerkTokenValidator;
import net.javaguides.Banking_app.security.JwtTokenProvider;
import net.javaguides.Banking_app.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final ClerkTokenValidator clerkTokenValidator;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPasswordHash;

    public AuthController(JwtTokenProvider jwtTokenProvider,
                          ClerkTokenValidator clerkTokenValidator,
                          UserService userService,
                          PasswordEncoder passwordEncoder) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.clerkTokenValidator = clerkTokenValidator;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    // ── Admin Login ─────────────────────────────────────────────────
    @PostMapping("/admin/login")
    public ResponseEntity<AuthResponse> adminLogin(@Valid @RequestBody AuthRequest request) {
        log.info("Admin login attempt for email: {}", request.getEmail());
        if (adminEmail.equalsIgnoreCase(request.getEmail()) &&
                passwordEncoder.matches(request.getPassword(), adminPasswordHash)) {

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    request.getEmail(), null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));

            String token = jwtTokenProvider.generateToken(authentication);
            return ResponseEntity.ok(new AuthResponse(token, request.getEmail(), Collections.singletonList("ROLE_ADMIN")));
        }
        log.warn("Invalid admin login credentials for email: {}", request.getEmail());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    // ── Clerk Login / Signup ─────────────────────────────────────────
    @PostMapping("/clerk-login")
    public ResponseEntity<AuthResponse> clerkLogin(@RequestBody AuthRequest request) {
        log.info("Clerk authentication verification started");
        String clerkToken = request.getClerkToken();
        if (clerkToken == null || clerkToken.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Claims claims = clerkTokenValidator.verifyAndExtractClaims(clerkToken);
        if (claims == null) {
            log.warn("Clerk token verification failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Extract identity from Clerk JWT
        String clerkUserId = claims.getSubject();
        String email = claims.get("email", String.class);
        if (email == null) {
            email = claims.get("email_address", String.class);
        }
        if (email == null) {
            email = clerkUserId + "@clerk.user";
        }
        String name = claims.get("name", String.class);
        if (name == null) {
            String firstName = claims.get("first_name", String.class);
            String lastName = claims.get("last_name", String.class);
            if (firstName != null) {
                name = lastName != null ? firstName + " " + lastName : firstName;
            } else {
                name = email.split("@")[0];
            }
        }

        // Register or sync user — phone is NOT set here (user will do it in setup step)
        UserDto userDto = userService.registerOrUpdateUser(clerkUserId, name, email, null);

        // Build JWT for our backend
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDto.getEmail(), null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

        String jwt = jwtTokenProvider.generateToken(authentication);

        // If the user has no phone yet, signal the frontend to show the phone setup screen
        boolean needsPhoneSetup = (userDto.getPhoneNumber() == null || userDto.getPhoneNumber().isBlank());
        log.info("Clerk login success for {} — needsPhoneSetup={}", email, needsPhoneSetup);

        AuthResponse response = new AuthResponse(jwt, userDto.getEmail(), Collections.singletonList("ROLE_USER"), needsPhoneSetup);
        return ResponseEntity.ok(response);
    }

    // ── Phone Setup (called after Clerk login for first-time users) ──
    @PostMapping("/setup-phone")
    public ResponseEntity<AuthResponse> setupPhone(Authentication authentication,
                                                    @Valid @RequestBody PhoneSetupRequest request) {
        String email = authentication.getName();
        log.info("Phone setup request for user: {}", email);
        try {
            UserDto userDto = userService.setupPhone(email, request.getPhoneNumber());
            // Return a fresh response confirming setup is done
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userDto.getEmail(), null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
            String jwt = jwtTokenProvider.generateToken(auth);
            return ResponseEntity.ok(new AuthResponse(jwt, userDto.getEmail(), Collections.singletonList("ROLE_USER"), false));
        } catch (IllegalArgumentException e) {
            log.warn("Phone setup failed for {}: {}", email, e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    // ── Demo / Guest Access (simulated login — no real auth required) ──
    @PostMapping("/simulated-login")
    public ResponseEntity<AuthResponse> simulatedLogin(@RequestBody AuthRequest request) {
        log.info("Demo/guest login for email: {}", request.getEmail());
        String email = request.getEmail();
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String clerkUserId = "demo_" + Math.abs(email.hashCode());
        String name = (request.getName() != null && !request.getName().trim().isEmpty())
                ? request.getName().trim()
                : email.split("@")[0];
        if (!name.isEmpty()) name = name.substring(0, 1).toUpperCase() + name.substring(1);

        // For demo mode, generate a phone number so the account is usable immediately
        String phoneNumber = "9" + String.format("%09d", Math.abs(email.hashCode() % 1000000000L));
        // Ensure uniqueness collision is handled gracefully
        UserDto userDto = userService.registerOrUpdateUser(clerkUserId, name, email, phoneNumber);

        // If the demo user was just created without a phone (shouldn't happen now), do setup
        if (userDto.getPhoneNumber() == null) {
            try {
                userDto = userService.setupPhone(email, phoneNumber);
            } catch (Exception ignored) {
                // Phone may already exist for another demo account — just continue
            }
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userDto.getEmail(), null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

        String jwt = jwtTokenProvider.generateToken(authentication);
        // Demo users always have a phone set, so needsPhoneSetup=false
        return ResponseEntity.ok(new AuthResponse(jwt, userDto.getEmail(), List.of("ROLE_USER"), false));
    }
}
