package com.swipehire.web.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** One OTP sent to a college email. Only the BCrypt hash of the code is stored. */
@Entity
@Table(name = "email_verification")
public class EmailVerification {

    public static final int MAX_ATTEMPTS = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 190)
    private String email;

    @Column(name = "otp_hash", nullable = false, length = 100)
    private String otpHash;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected EmailVerification() {
    }

    public EmailVerification(Long userId, String email, String otpHash, LocalDateTime expiresAt) {
        this.userId = userId;
        this.email = email;
        this.otpHash = otpHash;
        this.expiresAt = expiresAt;
    }

    public boolean isUsable(LocalDateTime now) {
        return verifiedAt == null && attempts < MAX_ATTEMPTS && now.isBefore(expiresAt);
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getOtpHash() { return otpHash; }
    public int getAttempts() { return attempts; }
    public void incrementAttempts() { attempts++; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
}
