package net.javaguides.Banking_app.security;
// ↑ Belongs to the "security" package.

import jakarta.servlet.FilterChain;
// ↑ FilterChain is the chain of HTTP filters. After this filter runs, we call
//   filterChain.doFilter() to pass the request to the NEXT filter in the chain.

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
// ↑ HttpServletRequest represents the incoming HTTP request (headers, body, URL, etc.)

import jakarta.servlet.http.HttpServletResponse;
// ↑ HttpServletResponse represents the outgoing HTTP response.

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// ↑ A Spring Security object that represents an authenticated user.
//   Contains: username, password (null here), and a list of authorities/roles.

import org.springframework.security.core.authority.SimpleGrantedAuthority;
// ↑ Represents a single role/permission as a simple string wrapper (e.g., "ROLE_USER").

import org.springframework.security.core.context.SecurityContextHolder;
// ↑ The SecurityContext is like a "security clipboard" for the current HTTP request.
//   We store the authenticated user's info here so Spring Security can check it later.

import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
// ↑ Used to add extra request details (like IP address) to the authentication token.

import org.springframework.stereotype.Component;
// ↑ @Component → Spring registers this filter as a bean.

import org.springframework.util.StringUtils;
// ↑ StringUtils.hasText() → returns true if a string is not null and not blank.

import org.springframework.web.filter.OncePerRequestFilter;
// ↑ OncePerRequestFilter is a Spring base class.
//   It guarantees that this filter runs EXACTLY ONCE per HTTP request.
//   (Some filter architectures run filters multiple times — this prevents that.)

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

// ============================================================
// WHAT IS JwtAuthenticationFilter?
// This is a SECURITY FILTER — it runs on EVERY single HTTP request before it
// reaches a Controller. Think of it as a security checkpoint.
//
// WHAT IT DOES (per request):
//   1. Read the "Authorization" header from the HTTP request.
//   2. If the header has "Bearer <token>", extract the token part.
//   3. Validate the token (is it signed correctly? is it expired?).
//   4. If valid, extract the username (email) and roles from the token.
//   5. Store user info in the SecurityContext (Spring Security's memory for this request).
//   6. Pass the request along to the next filter/controller.
//
// WHY IS THIS NEEDED?
// JWT tokens are STATELESS. The server has no "session" to remember you.
// So every request must prove its identity by sending the JWT.
// This filter is the "bouncer" that checks the token each time.
//
// ANALOGY:
// Think of the JWT as a concert wristband.
// Every time you want to enter (make a request), you show your wristband.
// This filter is the security guard who checks if the wristband is real.
// ============================================================

// @Component → Spring creates and manages this filter as a bean.
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    // "extends OncePerRequestFilter" → This filter runs exactly once per request.

    private final JwtTokenProvider tokenProvider;
    // JwtTokenProvider has the methods to validate and read JWT tokens.

    // Constructor injection — Spring provides the JwtTokenProvider automatically.
    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    // @Override → Implementing the abstract method from OncePerRequestFilter.
    // This is THE MAIN METHOD — it runs on every HTTP request.
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // ── STEP 1: Extract JWT from request header ─────────────────────
        // Looks for: "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
        // Returns just the token part, or null if header is missing.
        String token = getJwtFromRequest(request);

        // ── STEP 2: Validate the token ───────────────────────────────────
        // StringUtils.hasText(token) → true if token is not null/empty
        // tokenProvider.validateToken(token) → true if token is valid (not expired, not tampered)
        if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {

            // ── STEP 3: Extract user info from token ─────────────────────
            String username = tokenProvider.getUsernameFromJWT(token); // e.g., "aryan@gmail.com"
            List<String> roles = tokenProvider.getRolesFromJWT(token); // e.g., ["ROLE_USER"]

            // ── STEP 4: Convert roles → Spring Security authority objects ─
            // Stream the role strings, wrap each in SimpleGrantedAuthority.
            // Example: "ROLE_USER" → SimpleGrantedAuthority("ROLE_USER")
            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            // ── STEP 5: Create a Spring Security "authentication" token ───
            // UsernamePasswordAuthenticationToken(username, password, authorities):
            //   - username: the email extracted from JWT
            //   - password: null (we don't need it after JWT validation — already authenticated!)
            //   - authorities: the list of roles this user has
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    username, null, authorities);

            // Add extra request details (like the IP address) to the authentication object.
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // ── STEP 6: Store authenticated user in SecurityContext ────────
            // SecurityContextHolder stores the current user's authentication for this request thread.
            // After this line, Spring Security "knows" the current user for the rest of the request.
            // Controllers can then access this via: Authentication auth in method parameters.
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        // If token is missing or invalid → we don't set any authentication.
        // Spring Security will treat this request as "anonymous" and block protected endpoints.

        // ── STEP 7: Continue to the next filter/controller ───────────────
        // No matter what (valid or invalid token), we pass the request along the filter chain.
        // Spring Security will handle blocking unauthorized requests AFTER this filter.
        filterChain.doFilter(request, response);
    }

    // HELPER METHOD: Extract the JWT token from the "Authorization" HTTP header.
    private String getJwtFromRequest(HttpServletRequest request) {
        // Get the "Authorization" header value.
        // Example: "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhcnlhbkBnbWFpbC5jb20i..."
        String bearerToken = request.getHeader("Authorization");

        // Check:
        //   StringUtils.hasText(bearerToken) → header is not null/empty
        //   bearerToken.startsWith("Bearer ") → header starts with "Bearer " prefix
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            // Remove the "Bearer " prefix (7 characters) and return just the token part.
            // "Bearer eyJhbGci..." → "eyJhbGci..."
            return bearerToken.substring(7);
        }

        // If header is missing or doesn't start with "Bearer ", return null.
        // The calling code will handle null by skipping authentication.
        return null;
    }
}
