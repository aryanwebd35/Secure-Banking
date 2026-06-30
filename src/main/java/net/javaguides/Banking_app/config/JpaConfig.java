package net.javaguides.Banking_app.config;
// ↑ Belongs to the "config" package — configuration classes that set up Spring features.

import org.springframework.context.annotation.Configuration;
// ↑ @Configuration tells Spring: "This class defines Spring configuration beans or enables features."

import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
// ↑ @EnableJpaAuditing activates Spring Data's JPA Auditing feature.
//   "Auditing" = automatically tracking who created/modified a record and when.

// ============================================================
// WHAT IS JpaConfig?
// A configuration class that enables ONE important Spring Boot feature: JPA Auditing.
//
// WHAT IS JPA AUDITING?
// JPA Auditing automatically fills in "createdAt" and "updatedAt" fields
// on your database entities WITHOUT you having to manually set them.
//
// WITHOUT JPA Auditing — you'd have to write this in every service method:
//   account.setCreatedAt(LocalDateTime.now());   // Manual and error-prone!
//
// WITH JPA Auditing (@EnableJpaAuditing + @CreatedDate in BaseEntity):
//   Spring automatically fills in createdAt when saving a new record. 🎉
//   Spring automatically updates updatedAt every time a record is saved. 🎉
//
// HOW IT CONNECTS:
//   JpaConfig.java → enables auditing (@EnableJpaAuditing)
//   BaseEntity.java → uses auditing annotations (@CreatedDate, @LastModifiedDate)
//   Account.java, User.java, Transaction.java → extend BaseEntity → get the fields
//
// WHY IS THIS A SEPARATE CLASS?
// @EnableJpaAuditing requires being on a @Configuration class.
// Putting it in BankingAppApplication.java would work too, but separating it
// into its own config class is cleaner and more organized.
// ============================================================

// @Configuration → This class provides Spring configuration.
// @EnableJpaAuditing → Activates automatic date tracking (createdAt, updatedAt) on entities.
@Configuration
@EnableJpaAuditing
public class JpaConfig {
    // This class is intentionally empty!
    // The annotations above do ALL the work — no method bodies needed.
    // @EnableJpaAuditing is the "switch" that turns on automatic date auditing.
}
