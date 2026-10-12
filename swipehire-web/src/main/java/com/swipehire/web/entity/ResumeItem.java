package com.swipehire.web.entity;

import com.swipehire.common.enums.ResumeItemType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Set;

/** One structured resume entry. Tailoring selects and reorders these; it never edits them. */
@Entity
@Table(name = "resume_item")
public class ResumeItem {

    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM yyyy");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 16)
    private ResumeItemType itemType;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(length = 160)
    private String organization;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @ManyToMany
    @JoinTable(name = "resume_item_skill",
            joinColumns = @JoinColumn(name = "resume_item_id"),
            inverseJoinColumns = @JoinColumn(name = "skill_id"))
    private Set<Skill> skills = new LinkedHashSet<>();

    protected ResumeItem() {
    }

    public ResumeItem(Long applicantId, ResumeItemType itemType, String title) {
        this.applicantId = applicantId;
        this.itemType = itemType;
        this.title = title;
    }

    /** "Jan 2024 – Mar 2024", "Jan 2024 – Present" or "" for display. */
    public String getDateRange() {
        if (startDate == null) {
            return endDate == null ? "" : endDate.format(MONTH);
        }
        return startDate.format(MONTH) + " – " + (endDate == null ? "Present" : endDate.format(MONTH));
    }

    public Long getId() { return id; }
    public Long getApplicantId() { return applicantId; }
    public ResumeItemType getItemType() { return itemType; }
    public void setItemType(ResumeItemType itemType) { this.itemType = itemType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public Set<Skill> getSkills() { return skills; }
}
