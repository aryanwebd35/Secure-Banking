package net.javaguides.Banking_app.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "otp_verifications")
public class OtpVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    /** User's full name — stored during OTP request so we can create the user after verification */
    @Column(nullable = false)
    private String name;

    /** 6-digit OTP code */
    @Column(name = "otp_code", nullable = false, length = 6)
    private String otpCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** OTP expires 10 minutes after creation */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** True once the user has successfully verified this OTP */
    @Column(nullable = false)
    private boolean verified = false;

    public OtpVerification(String email, String name, String otpCode) {
        this.email     = email;
        this.name      = name;
        this.otpCode   = otpCode;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusMinutes(10);
        this.verified  = false;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
}
