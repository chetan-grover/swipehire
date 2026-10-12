package com.swipehire.web.form;

import com.swipehire.common.enums.JobType;
import com.swipehire.common.enums.WorkMode;
import com.swipehire.web.entity.Job;
import com.swipehire.web.entity.JobSkill;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Job posting form. Skills come from a table of rows in the JSP: a checkbox "skillIds", a
 * "required" checkbox "requiredSkillIds" and a select named "weight_{skillId}" per row; the
 * controller collects the weights into {@link #weights}.
 */
public class JobForm {

    private String title;
    private String description;
    private JobType jobType = JobType.INTERNSHIP;
    private WorkMode workMode = WorkMode.ONSITE;
    private String city;
    private int stipendMin;
    private int stipendMax;
    private int openings = 1;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate deadline = LocalDate.now().plusDays(30);
    private Integer minGradYear;
    private Integer maxGradYear;
    private boolean verifiedOnly;
    private boolean publish = true;
    private List<Long> skillIds = new ArrayList<>();
    private List<Long> requiredSkillIds = new ArrayList<>();
    private List<Long> allowedCollegeIds = new ArrayList<>();
    private Map<Long, Integer> weights = new LinkedHashMap<>();

    /** Pre-fills the form when editing an existing job. */
    public static JobForm from(Job job) {
        JobForm f = new JobForm();
        f.title = job.getTitle();
        f.description = job.getDescription();
        f.jobType = job.getJobType();
        f.workMode = job.getWorkMode();
        f.city = job.getCity();
        f.stipendMin = job.getStipendMin();
        f.stipendMax = job.getStipendMax();
        f.openings = job.getOpenings();
        f.deadline = job.getDeadline();
        f.minGradYear = job.getMinGradYear();
        f.maxGradYear = job.getMaxGradYear();
        f.verifiedOnly = job.isVerifiedOnly();
        for (JobSkill js : job.getSkills()) {
            f.skillIds.add(js.getSkill().getId());
            f.weights.put(js.getSkill().getId(), js.getWeight());
            if (js.isRequired()) {
                f.requiredSkillIds.add(js.getSkill().getId());
            }
        }
        job.getAllowedColleges().forEach(c -> f.allowedCollegeIds.add(c.getId()));
        return f;
    }

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
    public boolean isPublish() { return publish; }
    public void setPublish(boolean publish) { this.publish = publish; }
    public List<Long> getSkillIds() { return skillIds; }
    public void setSkillIds(List<Long> skillIds) { this.skillIds = skillIds; }
    public List<Long> getRequiredSkillIds() { return requiredSkillIds; }
    public void setRequiredSkillIds(List<Long> requiredSkillIds) { this.requiredSkillIds = requiredSkillIds; }
    public List<Long> getAllowedCollegeIds() { return allowedCollegeIds; }
    public void setAllowedCollegeIds(List<Long> allowedCollegeIds) { this.allowedCollegeIds = allowedCollegeIds; }
    public Map<Long, Integer> getWeights() { return weights; }
    public void setWeights(Map<Long, Integer> weights) { this.weights = weights; }
}
