package net.javaguides.Banking_app.security;
// ↑ This file is in the "security" package — handles authentication and authorization.

import io.jsonwebtoken.Claims;
// ↑ Claims is the "body" of a JWT token — it holds the data stored inside it
//   (like username, roles, expiry time).

import io.jsonwebtoken.Jwts;
// ↑ Jwts is the main builder/parser from the JJWT library for creating and reading JWT tokens.

import io.jsonwebtoken.SignatureAlgorithm;
// ↑ Defines the algorithm used to SIGN the JWT. We use HS256 (HMAC with SHA-256).

import io.jsonwebtoken.security.Keys;
// ↑ Keys is a helper to create secure cryptographic keys from a string secret.

import org.springframework.beans.factory.annotation.Value;
// ↑ @Value reads configuration from application.properties and injects it into a field.
//   Example: @Value("${app.jwt.secret}") injects the value of "app.jwt.secret" property.

import org.springframework.security.core.Authentication;
// ↑ Authentication holds info about who is currently logged in (username, roles).

import org.springframework.security.core.GrantedAuthority;
// ↑ GrantedAuthority represents a role/permission granted to a user (e.g., "ROLE_USER").

import org.springframework.stereotype.Component;
// ↑ @Component marks this as a Spring bean. Spring creates one instance and manages it.
//   @Component is the generic version. @Service, @Repository are specialized versions.

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

// ============================================================
// WHAT IS JwtTokenProvider?
// This class handles everything related to JWT (JSON Web Token) for our own app:
//   1. GENERATING a JWT when a user logs in
//   2. EXTRACTING the username from a JWT
//   3. EXTRACTING roles from a JWT
//   4. VALIDATING whether a JWT is valid (not expired, not tampered with)
//
// WHAT IS A JWT?
// A JWT is a compact, self-contained string used for authentication.
// It looks like: xxxxx.yyyyy.zzzzz (three Base64-encoded parts separated by dots)
//   Part 1 (Header): Algorithm and token type
//   Part 2 (Payload/Claims): The actual data (username, roles, expiry)
//   Part 3 (Signature): Cryptographic signature to prove the token wasn't tampered with
//
// HOW IT WORKS IN THIS APP:
//   1. User logs in → JwtTokenProvider.generateToken() creates a JWT
//   2. Frontend stores this JWT in localStorage
//   3. Every HTTP request → Frontend sends: "Authorization: Bearer <token>"
//   4. JwtAuthenticationFilter reads the token, validates it, extracts user info
//   5. Spring Security recognizes the user as authenticated for that request
//
// JWT IS STATELESS: The server doesn't store sessions.
// The token itself contains all the info needed to authenticate the user.
// ============================================================

// @Component → Spring will create and manage one instance of this class.
//   It can then be injected into other classes (like JwtAuthenticationFilter).
@Component
public class JwtTokenProvider {

    // @Value("${app.jwt.secret}") → Read the "app.jwt.secret" value from application.properties
    // The value is: "9a4f2c8d3b7a5e1f8c6b4d2e0f1a3c5e7g9h0i2j4k6l8m0n2o4p6q8r0s2t4u6v"
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    // @Value("${app.jwt.expiration-ms}") → Read the expiration time in milliseconds.
    // The value is: 86400000 (= 24 hours in milliseconds: 24 × 60 × 60 × 1000)
    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationInMs;

    // PRIVATE HELPER: Convert the secret string into a proper cryptographic Key object.
    // This Key is used both for signing (creating) tokens and verifying (reading) tokens.
    private Key getSignKey() {
        // Convert the secret string to raw bytes (UTF-8 encoding).
        byte[] keyBytes = this.jwtSecret.getBytes(StandardCharsets.UTF_8);
        // Create an HMAC-SHA key from those bytes. JJWT validates the key length automatically.
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ─── METHOD 1: Generate a JWT token ─────────────────────────────────
    // Called when a user successfully logs in. Returns a JWT string.
    // authentication parameter contains: who is logged in and their roles.
    public String generateToken(Authentication authentication) {
        // Get the username (= email in our app) from the authentication object.
        String username = authentication.getName();

        Date currentDate = new Date();                                       // Current timestamp
        Date expireDate = new Date(currentDate.getTime() + jwtExpirationInMs); // Expiry = now + 24h

        // Extract roles from authentication (e.g., ["ROLE_USER"] or ["ROLE_ADMIN"]).
        // authentication.getAuthorities() → returns a collection of GrantedAuthority objects
        // .stream().map(GrantedAuthority::getAuthority) → extract just the string names
        // .collect(Collectors.toList()) → collect into a List<String>
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        // Build the JWT token using the builder pattern:
        return Jwts.builder()
                .setSubject(username)         // Part of Payload: "sub": "aryan@gmail.com"
                .claim("roles", roles)        // Part of Payload: "roles": ["ROLE_USER"]
                .setIssuedAt(new Date())      // Part of Payload: "iat": <timestamp>
                .setExpiration(expireDate)    // Part of Payload: "exp": <expiry timestamp>
                .signWith(getSignKey(), SignatureAlgorithm.HS256) // Sign with HMAC-SHA256
                .compact();                  // Build into a compact string "xxxxx.yyyyy.zzzzz"
    }

    // ─── METHOD 2: Extract username from a JWT token ─────────────────────
    // Called by JwtAuthenticationFilter to identify WHO sent this request.
    public String getUsernameFromJWT(String token) {
        // Jwts.parserBuilder() creates a parser.
        // .setSigningKey(getSignKey()) → use the same key to VERIFY the signature.
        // .build().parseClaimsJws(token) → parse and validate the token.
        // If token is expired or tampered → throws an exception!
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody(); // .getBody() returns the Claims (payload) object

        // .getSubject() → extracts the "sub" field from the payload (= the username/email).
        return claims.getSubject();
    }

    // ─── METHOD 3: Extract roles from a JWT token ────────────────────────
    // Called by JwtAuthenticationFilter to know what the user is authorized to do.
    public List<String> getRolesFromJWT(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        // claims.get("roles", List.class) → get the "roles" field from the payload as a List.
        return claims.get("roles", List.class);
    }

    // ─── METHOD 4: Validate a JWT token ──────────────────────────────────
    // Returns TRUE if the token is valid. Returns FALSE if expired, malformed, or tampered.
    // Called by JwtAuthenticationFilter BEFORE trying to read the token contents.
    public boolean validateToken(String token) {
        try {
            // If parsing succeeds without throwing an exception → token is valid.
            Jwts.parserBuilder()
                    .setSigningKey(getSignKey())
                    .build()
                    .parseClaimsJws(token);
            return true; // Token is VALID

        } catch (Exception ex) {
            // Any exception means the token is invalid:
            //   ExpiredJwtException → token has expired
            //   MalformedJwtException → token is not a valid JWT format
            //   SignatureException → someone tampered with the token
            // We return false silently (no error message) — the filter will block the request.
        }
        return false; // Token is INVALID
    }
}
