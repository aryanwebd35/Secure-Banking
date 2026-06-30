package net.javaguides.Banking_app.security;
// ↑ Belongs to the "security" package.

import org.springframework.context.annotation.Bean;
// ↑ @Bean tells Spring: "This method returns an object that Spring should manage."
//   Spring calls this method and stores the returned object in its container.
//   Other classes can then inject this bean via constructor injection.

import org.springframework.context.annotation.Configuration;
// ↑ @Configuration tells Spring: "This class defines Spring configuration."
//   It's like a settings file for the app — Spring reads it at startup.

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
// ↑ AuthenticationManager is the central Spring Security component that processes login.
//   We expose it as a bean so AuthController can inject and use it.

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
// ↑ HttpSecurity is the main builder for configuring web security rules.

import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
// ↑ @EnableWebSecurity activates Spring Security for the web layer.
//   Without this, Spring Security is inactive.

import org.springframework.security.config.http.SessionCreationPolicy;
// ↑ SessionCreationPolicy.STATELESS tells Spring Security:
//   "Don't create HTTP sessions. Each request must authenticate via JWT (stateless)."

import org.springframework.security.web.SecurityFilterChain;
// ↑ SecurityFilterChain is the ordered chain of security filters that runs on every request.

import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
// ↑ Spring's built-in filter for username/password login form. We add our JWT filter BEFORE it.

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
// ↑ CORS (Cross-Origin Resource Sharing) configuration.
//   CORS controls which websites (origins) can call our API.
//   Example: A frontend at "http://localhost:3000" needs permission to call "http://localhost:8080".
//   Without CORS settings, browsers BLOCK cross-origin API requests for security.

import java.util.List;

// ============================================================
// WHAT IS SecurityConfig?
// The MASTER security configuration class for the entire Spring Boot app.
// It defines:
//   1. Which endpoints are PUBLIC (no login required)
//   2. Which endpoints require ROLE_ADMIN
//   3. Which endpoints require ANY valid JWT
//   4. How Spring Security loads users from the database (DaoAuthenticationProvider
//      is auto-configured via Spring Boot — see UserServiceImpl + PasswordEncoderConfig)
//   5. Which filters run on each request (our JwtAuthenticationFilter)
//   6. CORS settings (which frontend origins are allowed)
//
// NOTE ON CIRCULAR DEPENDENCY:
//   PasswordEncoder is defined in PasswordEncoderConfig (not here!) to avoid a cycle:
//   SecurityConfig → UserService → PasswordEncoder → SecurityConfig (cycle!)
//   By extracting PasswordEncoder to its own @Configuration class, Spring can
//   satisfy all dependencies without circular references.
//
// SECURITY RULES SUMMARY (from securityFilterChain method):
//   PUBLIC (no auth):
//     - / and static files (*.html, *.css, *.js)
//     - /api/auth/** (login, register)
//     - /actuator/** (health checks)
//     - /swagger-ui/** (API documentation)
//   ADMIN ONLY:
//     - /admin/** and /api/admin/** (requires ROLE_ADMIN)
//   ALL OTHER REQUESTS:
//     - Require a valid JWT token (any role)
// ============================================================

// @Configuration → This class provides Spring configuration (settings/beans).
// @EnableWebSecurity → Activate Spring Security for web HTTP requests.
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    // Our custom JWT filter that reads and validates the JWT token on every request.

    // Constructor injection — Spring provides the JwtAuthenticationFilter automatically.
    // NOTE: We do NOT inject UserService or PasswordEncoder here to avoid circular dependencies.
    // Spring Boot auto-configures DaoAuthenticationProvider because UserServiceImpl
    // implements UserDetailsService and PasswordEncoder is defined in PasswordEncoderConfig.
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    // ─── BEAN 1: Authentication Manager ──────────────────────────────────
    // @Bean → Spring manages this object.
    // AuthenticationManager is used in AuthController to validate login credentials.
    // Spring Boot auto-wires it with the DaoAuthenticationProvider because:
    //   - UserServiceImpl implements UserDetailsService (how to load users from DB)
    //   - PasswordEncoderConfig provides PasswordEncoder (how to verify passwords)
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    // ─── BEAN 2: CORS Configuration ───────────────────────────────────────
    // @Bean → Spring uses this to configure CORS headers on all responses.
    // CORS (Cross-Origin Resource Sharing) = a browser security mechanism.
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow requests from ANY origin (frontend URL like http://localhost:3000 or Vercel).
        // In production, restrict this to specific domains for better security.
        config.setAllowedOriginPatterns(List.of("*"));

        // Allow these HTTP methods from external origins.
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Allow any HTTP headers (like "Authorization", "Content-Type", etc.)
        config.setAllowedHeaders(List.of("*"));

        // Allow cookies/credentials to be included in cross-origin requests.
        config.setAllowCredentials(true);

        // Apply this CORS config to ALL URL patterns ("/**" = match everything).
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // ─── BEAN 3: Security Filter Chain (MAIN SECURITY RULES) ─────────────
    // @Bean → Spring builds and manages the security filter chain.
    // This is the MOST IMPORTANT method — it defines WHO can access WHAT.
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Apply our CORS configuration (from the method above).
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // DISABLE CSRF (Cross-Site Request Forgery) protection.
            // CSRF protection is for session-based apps (form logins).
            // Since we use JWT (stateless), CSRF is not needed and would break our API.
            .csrf(csrf -> csrf.disable())

            // STATELESS sessions: Don't create or use HTTP sessions.
            // Every request must authenticate via JWT. This is the "stateless" way.
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // AUTHORIZATION RULES: Who can access what?
            .authorizeHttpRequests(auth -> auth
                // ── PUBLIC routes (no authentication required) ──────────────────
                // Static files served by Spring Boot (frontend app).
                .requestMatchers("/", "/index.html", "/app.js", "/style.css", "/favicon.ico",
                        "/*.css", "/*.js", "/*.png", "/*.ico", "/*.svg", "/*.map").permitAll()

                // Authentication endpoints (register, login, admin login).
                // These MUST be public — users can't authenticate without being able to access them!
                .requestMatchers("/api/auth/**").permitAll()

                // Spring Actuator health/metrics endpoints (for monitoring).
                .requestMatchers("/actuator/**").permitAll()

                // Swagger (API documentation) endpoints.
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**", "/webjars/**").permitAll()

                // ── ADMIN ONLY routes ────────────────────────────────────────────
                // Only users with ROLE_ADMIN can access /admin/** endpoints.
                // Note: Spring Security automatically handles the "ROLE_" prefix.
                // .hasRole("ADMIN") checks for authority "ROLE_ADMIN".
                .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")

                // ── EVERYTHING ELSE requires authentication ─────────────────────
                // Any endpoint not listed above needs a valid JWT token.
                // This includes /api/accounts/**, /transfer, /transactions/**, etc.
                .anyRequest().authenticated()
            )

            // ADD OUR JWT FILTER before Spring's default username/password filter.
            // This means every request first goes through our JwtAuthenticationFilter,
            // which sets up authentication from the JWT token.
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Build and return the configured SecurityFilterChain.
        return http.build();
    }
}
