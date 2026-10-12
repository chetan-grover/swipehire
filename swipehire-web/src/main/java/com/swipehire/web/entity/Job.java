package com.swipehire.web.entity;

import com.swipehire.common.enums.JobStatus;
import com.swipehire.common.enums.JobType;
import com.swipehire.common.enums.WorkMode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "job")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "company_id")
    private Company company;

    /** Recruiter (hirer.user_id) who posted the job. */
    @Column(name = "posted_by", nullable = false)
    private Long postedBy;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 16)
    private JobType jobType = JobType.INTERNSHIP;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_mode", nullable = false, length = 16)
    private WorkMode workMode = WorkMode.ONSITE;

    @Column(length = 80)
    private String city;

    @Column(name = "stipend_min", nullable = false)
    private int stipendMin;

    @Column(name = "stipend_max", nullable = false)
    private int stipendMax;

    @Column(nullable = false)
    private int openings = 1;

    @Column(nullable = false)
    private LocalDate deadline;

    @Column(name = "min_grad_year")
    private Integer minGradYear;

    @Column(name = "max_grad_year")
    private Integer maxGradYear;

    @Column(name = "verified_only", nullable = false)
    private boolean verifiedOnly;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private JobStatus status = JobStatus.DRAFT;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JobSkill> skills = new ArrayList<>();

    /** Plain many-to-many: the junction table has no extra columns, so no entity is needed for it. */
    @ManyToMany
    @JoinTable(name = "job_allowed_college",
            joinColumns = @JoinColumn(name = "job_id"),
            inverseJoinColumns = @JoinColumn(name = "college_id"))
    private Set<College> allowedColleges = new LinkedHashSet<>();

    protected Job() {
    }

    public Job(Company company, Long postedBy) {
        this.company = company;
        this.postedBy = postedBy;
    }

    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isOpen() {
        return status == JobStatus.ACTIVE && !deadline.isBefore(LocalDate.now());
    }

    public Long getId() { return id; }
    public Company getCompany() { return company; }
    public Long getPostedBy() { return postedBy; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public JobType getJobType() { return jobType; }
    public void setJobType(JobType jobType) { this.jobType = jobType; }
    public WorkMode getWorkMode() { return workMode; }
    public void setWorkMode(WorkMode workMode) { this.workMode = workMode; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public int getStipendMin() { return stipendMin; }
    public void setStipendMin(int stipendMin) { this.stipendMin = stipendMin; }
    public int getStipendMax() { return stipendMax; }
    public void setStipendMax(int stipendMax) { this.stipendMax = stipendMax; }
    public int getOpenings() { return openings; }
    public void setOpenings(int openings) { this.openings = openings; }
    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }
    public Integer getMinGradYear() { return minGradYear; }
    public void setMinGradYear(Integer minGradYear) { this.minGradYear = minGradYear; }
    public Integer getMaxGradYear() { return maxGradYear; }
    public void setMaxGradYear(Integer maxGradYear) { this.maxGradYear = maxGradYear; }
    public boolean isVerifiedOnly() { return verifiedOnly; }
    public void setVerifiedOnly(boolean verifiedOnly) { this.verifiedOnly = verifiedOnly; }
    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public List<JobSkill> getSkills() { return skills; }
    public Set<College> getAllowedColleges() { return allowedColleges; }
}
