package com.swipehire.common.dto;

import com.swipehire.common.enums.JobType;
import com.swipehire.common.enums.WorkMode;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Everything the scorer needs to know about one job. Sent over RMI, so Serializable. */
public class JobData implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private long id;
    private String title = "";
    private String text = "";
    /** skillId -> weight (1..5). */
    private Map<Long, Integer> skillWeights = new HashMap<>();
    private Set<Long> requiredSkillIds = new HashSet<>();
    private JobType jobType = JobType.INTERNSHIP;
    private WorkMode workMode = WorkMode.ONSITE;
    private String city;
    private int stipendMin;
    private int stipendMax;
    private Integer minGradYear;
    private Integer maxGradYear;
    private boolean verifiedOnly;
    private Set<Long> allowedCollegeIds = new HashSet<>();

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text == null ? "" : text; }
    public Map<Long, Integer> getSkillWeights() { return skillWeights; }
    public void setSkillWeights(Map<Long, Integer> skillWeights) { this.skillWeights = skillWeights; }
    public Set<Long> getRequiredSkillIds() { return requiredSkillIds; }
    public void setRequiredSkillIds(Set<Long> requiredSkillIds) { this.requiredSkillIds = requiredSkillIds; }
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
    public Integer getMinGradYear() { return minGradYear; }
    public void setMinGradYear(Integer minGradYear) { this.minGradYear = minGradYear; }
    public Integer getMaxGradYear() { return maxGradYear; }
    public void setMaxGradYear(Integer maxGradYear) { this.maxGradYear = maxGradYear; }
    public boolean isVerifiedOnly() { return verifiedOnly; }
    public void setVerifiedOnly(boolean verifiedOnly) { this.verifiedOnly = verifiedOnly; }
    public Set<Long> getAllowedCollegeIds() { return allowedCollegeIds; }
    public void setAllowedCollegeIds(Set<Long> allowedCollegeIds) { this.allowedCollegeIds = allowedCollegeIds; }
}
