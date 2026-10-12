package com.swipehire.web.service;

import com.swipehire.common.enums.NotificationType;
import com.swipehire.common.enums.VerificationStatus;
import com.swipehire.web.config.AppTime;
import com.swipehire.web.entity.ApplicantProfile;
import com.swipehire.web.entity.College;
import com.swipehire.web.entity.EmailVerification;
import com.swipehire.web.repository.ApplicantProfileRepository;
import com.swipehire.web.repository.CollegeRepository;
import com.swipehire.web.repository.EmailVerificationRepository;
import com.swipehire.web.support.BusinessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

/**
 * College verification by email OTP:
 * enter college email -> domain must belong to a VERIFIED college -> 6-digit code mailed
 * (valid 10 minutes, 5 attempts, stored only as a BCrypt hash) -> correct code sets college_id.
 */
@Service
public class VerificationService {

    static final int OTP_VALID_MINUTES = 10;

    private final SecureRandom random = new SecureRandom();
    private final EmailVerificationRepository verifications;
    private final ApplicantProfileRepository profiles;
    private final CollegeRepository colleges;
    private final PasswordEncoder encoder;
    private final EmailService email;
    private final NotificationService notifications;

    public VerificationService(EmailVerificationRepository verifications, ApplicantProfileRepository profiles,
                               CollegeRepository colleges, PasswordEncoder encoder, EmailService email,
                               NotificationService notifications) {
        this.verifications = verifications;
        this.profiles = profiles;
        this.colleges = colleges;
        this.encoder = encoder;
        this.email = email;
        this.notifications = notifications;
    }

    /**
     * Sends a fresh OTP. Returns the code only when real email is disabled (dev/demo mode), so
     * the page can show it; in production the code exists only in the email.
     */
    @Transactional
    public Optional<String> sendOtp(long userId, String collegeEmail) {
        String addr = collegeEmail == null ? "" : collegeEmail.trim().toLowerCase(Locale.ROOT);
        int at = addr.indexOf('@');
        if (at < 1 || at == addr.length() - 1) {
            throw new BusinessException("Enter your full college email address");
        }
        College college = findCollegeForDomain(addr.substring(at + 1))
                .orElseThrow(() -> new BusinessException("We don't know the domain '" + addr.substring(at + 1)
                        + "' yet. Request your college below and an admin will add it."));
        if (college.getStatus() != VerificationStatus.VERIFIED) {
            throw new BusinessException(college.getName() + " is still awaiting admin approval");
        }
        ApplicantProfile me = profiles.findById(userId).orElseThrow();
        if (!addr.equalsIgnoreCase(me.getCollegeEmail()) && profiles.existsByCollegeEmailIgnoreCase(addr)) {
            throw new BusinessException("This college email is already verified by another account");
        }
        String code = String.format("%06d", random.nextInt(1_000_000));
        verifications.save(new EmailVerification(userId, addr, encoder.encode(code),
                AppTime.now().plusMinutes(OTP_VALID_MINUTES)));
        email.sendAsync(addr, "Your SwipeHire verification code",
                "Your code is " + code + ". It is valid for " + OTP_VALID_MINUTES + " minutes.\n"
                        + "If you did not request this, ignore this email.");
        return email.isEnabled() ? Optional.empty() : Optional.of(code);
    }

    /**
     * Sub-domains count too: "cse.vitstudent.ac.in" matches the college "vitstudent.ac.in".
     */
    Optional<College> findCollegeForDomain(String domain) {
        String d = domain;
        while (d.contains(".")) {
            Optional<College> c = colleges.findByEmailDomainIgnoreCase(d);
            if (c.isPresent()) {
                return c;
            }
            d = d.substring(d.indexOf('.') + 1);
        }
        return Optional.empty();
    }

    /**
     * noRollbackFor: a wrong code throws WrongOtpException, but the incremented attempt counter
     * must still be committed, otherwise someone could guess codes forever.
     */
    @Transactional(noRollbackFor = WrongOtpException.class)
    public College confirmOtp(long userId, String code) {
        EmailVerification v = verifications.findTopByUserIdOrderByCreatedAtDesc(userId)
                .orElseThrow(() -> new BusinessException("Request a code first"));
        LocalDateTime now = AppTime.now();
        if (!v.isUsable(now)) {
            throw new BusinessException("This code has expired or was tried too many times. Request a new one.");
        }
        v.incrementAttempts();
        if (code == null || !encoder.matches(code.trim(), v.getOtpHash())) {
            int left = EmailVerification.MAX_ATTEMPTS - v.getAttempts();
            throw new WrongOtpException("Wrong code. " + left + " attempt(s) left.");
        }
        v.setVerifiedAt(now);
        String domain = v.getEmail().substring(v.getEmail().indexOf('@') + 1);
        College college = findCollegeForDomain(domain).orElseThrow();
        ApplicantProfile me = profiles.findById(userId).orElseThrow();
        me.setCollege(college);
        me.setCollegeEmail(v.getEmail());
        me.setCollegeVerifiedAt(now);
        notifications.notify(userId, NotificationType.VERIFICATION, "You're a verified student of " + college.getName(),
                null, "/applicant/profile");
        return college;
    }

    /** Wrong OTP; the transaction still commits the attempt counter (see noRollbackFor above). */
    public static class WrongOtpException extends BusinessException {
        public WrongOtpException(String message) {
            super(message);
        }
    }

    @Transactional
    public void requestCollege(long userId, String name, String domain, String city) {
        String d = domain == null ? "" : domain.trim().toLowerCase(Locale.ROOT).replaceFirst("^@", "");
        if (name == null || name.isBlank() || !d.matches("^[a-z0-9.-]+\\.[a-z]{2,}$")) {
            throw new BusinessException("Enter the college name and its email domain, e.g. vitstudent.ac.in");
        }
        if (colleges.findByEmailDomainIgnoreCase(d).isPresent()) {
            throw new BusinessException("That domain is already registered or awaiting approval");
        }
        colleges.save(new College(name.trim(), d, AccountService.trimToNull(city), VerificationStatus.PENDING, userId));
    }

    /** Hourly cleanup of old, unused codes. */
    @Transactional
    public int purgeExpired() {
        return verifications.deleteExpiredBefore(AppTime.now().minusDays(1));
    }
}
