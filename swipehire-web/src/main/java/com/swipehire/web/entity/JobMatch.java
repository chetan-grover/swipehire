package com.swipehire.web.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * A mutual right-swipe. Rows are inserted with plain JDBC (INSERT IGNORE inside the swipe
 * transaction, see SwipeDao); this entity is used to read them, hence no generated id here.
 */
@Entity
@Table(name = "job_match")
public class JobMatch {

    @Id
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "applicant_id")
    private ApplicantProfile applicant;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "job_id")
    private Job job;

    @Column(name = "matched_at", nullable = false)
    private LocalDateTime matchedAt;

    protected JobMatch() {
    }

    public Long getId() { return id; }
    public ApplicantProfile getApplicant() { return applicant; }
    public Job getJob() { return job; }
    public LocalDateTime getMatchedAt() { return matchedAt; }
}
