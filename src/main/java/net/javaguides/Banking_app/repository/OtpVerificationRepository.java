package net.javaguides.Banking_app.repository;
// ↑ Belongs to the "repository" package — database access layer.

import net.javaguides.Banking_app.entity.OtpVerification;
// ↑ Imports OtpVerification entity (the "otp_verifications" table).

import org.springframework.data.jpa.repository.JpaRepository;
// ↑ Gives us FREE database methods (save, findById, delete, etc.)

import org.springframework.stereotype.Repository;
// ↑ @Repository marks this interface as a Spring Data repository bean.
//   When extending JpaRepository, Spring auto-creates the implementation.
//   This annotation is optional but good practice for clarity.

import java.util.Optional;
// ↑ Optional<T> — safe container, avoids NullPointerException.

// ============================================================
// WHAT IS OtpVerificationRepository?
// Database access layer for the "otp_verifications" table.
// Used during the OTP email verification flow.
//
// HOW THE OTP FLOW USES THIS REPOSITORY:
//   1. User requests OTP → Service creates OtpVerification, saves via .save()
//   2. User submits OTP → Service finds the latest unverified OTP via findTop...()
//   3. OTP is verified → Service deletes the OTP record via deleteByEmail()
// ============================================================

// @Repository → Marks this as a Spring repository bean. Spring auto-implements this interface.
@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {
    // ↑ Works with OtpVerification entities and Long primary key.

    // ============================================================
    // SPECIAL METHOD: findTopByEmailAndVerifiedFalseOrderByCreatedAtDesc
    // This is a DERIVED QUERY — Spring Data reads the METHOD NAME and generates SQL!
    //
    // NAME BREAKDOWN (read it like English):
    //   findTop          → Get only the FIRST/TOP result (not all matches)
    //   ByEmail          → WHERE email = ?
    //   AndVerifiedFalse → AND verified = false  (only unverified OTPs)
    //   OrderByCreatedAtDesc → ORDER BY created_at DESC (newest first)
    //
    // GENERATED SQL (roughly):
    //   SELECT * FROM otp_verifications
    //   WHERE email = ?
    //   AND verified = false
    //   ORDER BY created_at DESC
    //   LIMIT 1
    //
    // WHY? → A user might have requested multiple OTPs (e.g., by clicking "resend").
    //         We only want the MOST RECENT unverified OTP to validate against.
    /** Find the most recent unverified OTP for this email */
    Optional<OtpVerification> findTopByEmailAndVerifiedFalseOrderByCreatedAtDesc(String email);

    // ============================================================
    // CLEANUP METHOD: deleteByEmail
    // Spring Data auto-generates:
    //   DELETE FROM otp_verifications WHERE email = ?
    //
    // WHY? → After a user successfully verifies their OTP, we clean up all OTP records
    //         for that email. This keeps the database tidy and prevents old OTPs from
    //         being re-used.
    /** Delete all OTP records for an email (cleanup after verification) */
    void deleteByEmail(String email);
}
