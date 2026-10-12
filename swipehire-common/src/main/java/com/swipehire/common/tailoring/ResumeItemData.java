package com.swipehire.common.tailoring;

import com.swipehire.common.enums.ResumeItemType;

import java.io.Serial;
import java.io.Serializable;
import java.util.Set;

/** One structured resume entry written by the applicant (a project, internship, award...). */
public class ResumeItemData implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final long id;
    private final ResumeItemType type;
    private final String title;
    private final String organization;
    private final String dateRange;
    private final String description;
    private final Set<Long> skillIds;
    /** The applicant's own ordering, used as a tie-breaker so equal items keep their original order. */
    private final int displayOrder;

    public ResumeItemData(long id, ResumeItemType type, String title, String organization, String dateRange,
                          String description, Set<Long> skillIds, int displayOrder) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.organization = organization;
        this.dateRange = dateRange;
        this.description = description == null ? "" : description;
        this.skillIds = Set.copyOf(skillIds);
        this.displayOrder = displayOrder;
    }

    public long getId() { return id; }
    public ResumeItemType getType() { return type; }
    public String getTitle() { return title; }
    public String getOrganization() { return organization; }
    public String getDateRange() { return dateRange; }
    public String getDescription() { return description; }
    public Set<Long> getSkillIds() { return skillIds; }
    public int getDisplayOrder() { return displayOrder; }
}
