package com.swipehire.web.entity;

import com.swipehire.common.enums.ApplicationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** The pipeline opened by a match (1:1 with job_match). Status changes go through PipelineService only. */
@Entity
@Table(name = "application")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "match_id", unique = true)
    private JobMatch match;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ApplicationStatus status = ApplicationStatus.MATCHED;

    @Column(name = "tailored_pdf_path", length = 255)
    private String tailoredPdfPath;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;

    @Column(name = "status_changed_at", nullable = false)
    private LocalDateTime statusChangedAt = LocalDateTime.now();

    protected Application() {
    }

    public Application(JobMatch match) {
        this.match = match;
    }

    public Long getId() { return id; }
    public JobMatch getMatch() { return match; }
    public ApplicationStatus getStatus() { return status; }
    public void setStatus(ApplicationStatus status) {
        this.status = status;
        this.statusChangedAt = LocalDateTime.now();
    }
    public String getTailoredPdfPath() { return tailoredPdfPath; }
    public void setTailoredPdfPath(String tailoredPdfPath) { this.tailoredPdfPath = tailoredPdfPath; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
    public LocalDateTime getStatusChangedAt() { return statusChangedAt; }
}
