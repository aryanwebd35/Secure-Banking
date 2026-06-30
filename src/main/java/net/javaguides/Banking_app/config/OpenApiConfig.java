package net.javaguides.Banking_app.config;
// ↑ Belongs to the "config" package.

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
// ↑ All imports from SpringDoc OpenAPI library — used to configure the Swagger UI.

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// ============================================================
// WHAT IS OpenApiConfig?
// Configures Swagger UI — an interactive web-based API documentation tool.
//
// WHAT IS SWAGGER UI?
// Swagger (now called OpenAPI) automatically generates a web page where you can:
//   1. See ALL your API endpoints listed with their request/response formats.
//   2. Try out API calls directly from the browser (no Postman needed!).
//   3. See what authentication is required.
//
// HOW TO ACCESS:
//   After starting the app, open: http://localhost:8080/swagger-ui/index.html
//
// WHAT THIS CONFIG ADDS:
//   - A title and description for the API documentation
//   - A "Authorize" button in Swagger UI that lets you enter a JWT token
//   - Once you enter the token, all Swagger API calls will include it automatically
//
// The "bearerAuth" security scheme tells Swagger:
//   "Send the JWT as: Authorization: Bearer <token>"
// ============================================================

// @Configuration → This class defines Spring beans (Swagger configuration objects).
@Configuration
public class OpenApiConfig {

    // @Bean → Spring manages the OpenAPI object returned here.
    //   The SpringDoc library (springdoc-openapi) reads this bean and configures Swagger UI.
    @Bean
    public OpenAPI customOpenAPI() {
        // "bearerAuth" is just a NAME we give to our security scheme (can be anything).
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
                // API documentation metadata:
                .info(new Info()
                        .title("Digital Banking Platform API")       // Title shown in Swagger UI
                        .version("1.0")                              // API version number
                        .description("Backend APIs for Digital Banking System with JWT Authentication, Admin roles, and peer-to-peer transfers."))

                // Add a security REQUIREMENT: all endpoints require "bearerAuth" by default.
                // This shows the "lock" icon on every endpoint in Swagger UI.
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))

                // Define HOW the "bearerAuth" security works:
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP) // It's an HTTP auth scheme
                                        .scheme("bearer")               // The scheme is "Bearer"
                                        .bearerFormat("JWT")));         // The token format is JWT
        // Result: Swagger will send "Authorization: Bearer <your-token>" with every request.
    }
}
