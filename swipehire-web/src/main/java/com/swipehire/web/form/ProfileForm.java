package com.swipehire.web.form;

import com.swipehire.common.enums.JobType;
import com.swipehire.common.enums.WorkMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** Backing object for the applicant profile form (Spring MVC binds request params to it). */
public class ProfileForm {

    @Size(max = 120)
    private String fullName;
    @Size(max = 160)
    private String headline;
    @Size(max = 2000)
    private String about;
    @Size(max = 20)
    private String phone;
    @Size(max = 80)
    private String city;
    @Size(max = 100)
    private String degree;
    @Min(1990)
    @Max(2040)
    private Integer gradYear;
    private WorkMode prefWorkMode = WorkMode.ANY;
    private JobType prefJobType = JobType.ANY;
    @Min(0)
    private int minStipend;
    private boolean discoverable = true;
    /** Comma separated, e.g. "Bengaluru, Pune". */
    @Size(max = 400)
    private String preferredCities;

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }
    public String getAbout() { return about; }
    public void setAbout(String about) { this.about = about; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getDegree() { return degree; }
    public void setDegree(String degree) { this.degree = degree; }
    public Integer getGradYear() { return gradYear; }
    public void setGradYear(Integer gradYear) { this.gradYear = gradYear; }
    public WorkMode getPrefWorkMode() { return prefWorkMode; }
    public void setPrefWorkMode(WorkMode prefWorkMode) { this.prefWorkMode = prefWorkMode; }
    public JobType getPrefJobType() { return prefJobType; }
    public void setPrefJobType(JobType prefJobType) { this.prefJobType = prefJobType; }
    public int getMinStipend() { return minStipend; }
    public void setMinStipend(int minStipend) { this.minStipend = minStipend; }
    public boolean isDiscoverable() { return discoverable; }
    public void setDiscoverable(boolean discoverable) { this.discoverable = discoverable; }
    public String getPreferredCities() { return preferredCities; }
    public void setPreferredCities(String preferredCities) { this.preferredCities = preferredCities; }
}
