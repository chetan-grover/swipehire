package com.swipehire.web.repository;

import com.swipehire.web.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findTopByUserIdOrderByCreatedAtDesc(Long userId);

    /** HQL bulk delete used by the cleanup job: unverified codes that expired long ago. */
    @Modifying
    @Query("delete from EmailVerification v where v.verifiedAt is null and v.expiresAt < :cutoff")
    int deleteExpiredBefore(LocalDateTime cutoff);
}
