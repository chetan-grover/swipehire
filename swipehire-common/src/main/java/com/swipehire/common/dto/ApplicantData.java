package com.swipehire.common.dto;

import com.swipehire.common.enums.JobType;
import com.swipehire.common.enums.WorkMode;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Everything the scorer needs to know about one applicant. Serializable because it travels
 * from the web JVM to the engine JVM inside an RMI call.
 */
public class ApplicantData implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private long id;
    private Map<Long, Integer> skillProficiency = new HashMap<>();
    private Map<Long, Double> skillAffinity = new HashMap<>();
    private String text = "";
    /** Lower-cased city names. */
    private Set<String> preferredCities = new HashSet<>();
    private WorkMode prefWorkMode = WorkMode.ANY;
    private JobType prefJobType = JobType.ANY;
    private int minStipend;
    private Integer gradYear;
    private Long collegeId;
    private boolean collegeVerified;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public Map<Long, Integer> getSkillProficiency() { return skillProficiency; }
    public void setSkillProficiency(Map<Long, Integer> skillProficiency) { this.skillProficiency = skillProficiency; }
    public Map<Long, Double> getSkillAffinity() { return skillAffinity; }
    public void setSkillAffinity(Map<Long, Double> skillAffinity) { this.skillAffinity = skillAffinity; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text == null ? "" : text; }
    public Set<String> getPreferredCities() { return preferredCities; }
    public void setPreferredCities(Set<String> preferredCities) { this.preferredCities = preferredCities; }
    public WorkMode getPrefWorkMode() { return prefWorkMode; }
    public void setPrefWorkMode(WorkMode prefWorkMode) { this.prefWorkMode = prefWorkMode; }
    public JobType getPrefJobType() { return prefJobType; }
    public void setPrefJobType(JobType prefJobType) { this.prefJobType = prefJobType; }
    public int getMinStipend() { return minStipend; }
    public void setMinStipend(int minStipend) { this.minStipend = minStipend; }
    public Integer getGradYear() { return gradYear; }
    public void setGradYear(Integer gradYear) { this.gradYear = gradYear; }
    public Long getCollegeId() { return collegeId; }
    public void setCollegeId(Long collegeId) { this.collegeId = collegeId; }
    public boolean isCollegeVerified() { return collegeVerified; }
    public void setCollegeVerified(boolean collegeVerified) { this.collegeVerified = collegeVerified; }
}
