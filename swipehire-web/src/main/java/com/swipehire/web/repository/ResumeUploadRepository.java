package com.swipehire.web.repository;

import com.swipehire.web.entity.ResumeUpload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResumeUploadRepository extends JpaRepository<ResumeUpload, Long> {
    Optional<ResumeUpload> findTopByApplicantIdOrderByUploadedAtDesc(Long applicantId);
}
