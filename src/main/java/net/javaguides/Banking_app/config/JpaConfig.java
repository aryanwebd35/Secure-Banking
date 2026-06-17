package net.javaguides.Banking_app.config; // Defines the package where this configuration class belongs

import org.springframework.context.annotation.Configuration; // Imports Spring's Configuration helper
import org.springframework.data.jpa.repository.config.EnableJpaAuditing; // Imports the Auditing helper to auto-track date/time

// @Configuration tells Spring Boot: "Hey, this is a settings file where we define configuration options."
@Configuration 
// @EnableJpaAuditing tells Spring Boot: "Hey, automatically capture and save 'createdAt' and 'updatedAt' dates for database items."
@EnableJpaAuditing 
public class JpaConfig {
    // This class is empty because the annotations above do all the heavy lifting!
}

