package com.swipehire.web.entity;

import com.swipehire.common.enums.JobType;
import com.swipehire.common.enums.WorkMode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Applicant details, 1:1 with users. @MapsId makes the profile share the user's primary key
 * (user_id is both PK and FK), which is how the schema models the 1:1 relationship.
 */
@Entity
@Table(name = "applicant_profile")
public class ApplicantProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(length = 160)
    private String headline;

    @Column(columnDefinition = "TEXT")
    private String about;

    @Column(length = 20)
    private String phone;

    @Column(length = 80)
    private String city;

    @Column(length = 100)
    private String degree;

    @Column(name = "grad_year")
    private Integer gradYear;

    /** Set only after the college email OTP is verified. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "college_id")
    private College college;

    @Column(name = "college_email", length = 190, unique = true)
    private String collegeEmail;

    @Column(name = "college_verified_at")
    private LocalDateTime collegeVerifiedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "pref_work_mode", nullable = false, length = 16)
    private WorkMode prefWorkMode = WorkMode.ANY;

    @Enumerated(EnumType.STRING)
    @Column(name = "pref_job_type", nullable = false, length = 16)
    private JobType prefJobType = JobType.ANY;

    @Column(name = "min_stipend", nullable = false)
    private int minStipend;

    @Column(nullable = false)
    private boolean discoverable = true;

    @Column(name = "photo_path", length = 255)
    private String photoPath;

    /** A multi-valued attribute stored in its own table (applicant_pref_city) to keep 1NF. */
    @ElementCollection
    @CollectionTable(name = "applicant_pref_city", joinColumns = @JoinColumn(name = "applicant_id"))
    @Column(name = "city", length = 80)
    private Set<String> preferredCities = new LinkedHashSet<>();

    @OneToMany(mappedBy = "applicant", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("proficiency DESC")
    private List<ApplicantSkill> skills = new ArrayList<>();

    protected ApplicantProfile() {
    }

    public ApplicantProfile(User user) {
        this.user = user;
    }

    public boolean isCollegeVerified() {
        return college != null && collegeVerifiedAt != null;
    }

    public Long getUserId() { return userId; }
    public User getUser() { return user; }
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
    public College getCollege() { return college; }
    public void setCollege(College college) { this.college = college; }
    public String getCollegeEmail() { return collegeEmail; }
    public void setCollegeEmail(String collegeEmail) { this.collegeEmail = collegeEmail; }
    public LocalDateTime getCollegeVerifiedAt() { return collegeVerifiedAt; }
    public void setCollegeVerifiedAt(LocalDateTime collegeVerifiedAt) { this.collegeVerifiedAt = collegeVerifiedAt; }
    public WorkMode getPrefWorkMode() { return prefWorkMode; }
    public void setPrefWorkMode(WorkMode prefWorkMode) { this.prefWorkMode = prefWorkMode; }
    public JobType getPrefJobType() { return prefJobType; }
    public void setPrefJobType(JobType prefJobType) { this.prefJobType = prefJobType; }
    public int getMinStipend() { return minStipend; }
    public void setMinStipend(int minStipend) { this.minStipend = minStipend; }
    public boolean isDiscoverable() { return discoverable; }
    public void setDiscoverable(boolean discoverable) { this.discoverable = discoverable; }
    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }
    public Set<String> getPreferredCities() { return preferredCities; }
    public List<ApplicantSkill> getSkills() { return skills; }
}
