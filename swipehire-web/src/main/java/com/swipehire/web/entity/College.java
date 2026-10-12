package com.swipehire.web.entity;

import com.swipehire.common.enums.VerificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "college")
public class College {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "email_domain", nullable = false, length = 100, unique = true)
    private String emailDomain;

    @Column(length = 80)
    private String city;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private VerificationStatus status = VerificationStatus.PENDING;

    /** User who asked for this college to be added (null for seeded colleges). */
    @Column(name = "requested_by")
    private Long requestedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected College() {
    }

    public College(String name, String emailDomain, String city, VerificationStatus status, Long requestedBy) {
        this.name = name;
        this.emailDomain = emailDomain;
        this.city = city;
        this.status = status;
        this.requestedBy = requestedBy;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmailDomain() { return emailDomain; }
    public String getCity() { return city; }
    public VerificationStatus getStatus() { return status; }
    public void setStatus(VerificationStatus status) { this.status = status; }
    public Long getRequestedBy() { return requestedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
