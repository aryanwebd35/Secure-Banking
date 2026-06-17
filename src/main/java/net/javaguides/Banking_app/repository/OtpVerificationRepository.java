package net.javaguides.Banking_app.repository;

import net.javaguides.Banking_app.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    /** Find the most recent unverified OTP for this email */
    Optional<OtpVerification> findTopByEmailAndVerifiedFalseOrderByCreatedAtDesc(String email);

    /** Delete all OTP records for an email (cleanup after verification) */
    void deleteByEmail(String email);
}
