package com.swipehire.common.dto;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * The explainable result for one (applicant, job) pair: the total plus every component and the
 * skill lists behind it. This is what makes the ranking defensible: nothing is hidden.
 */
public class ScoreBreakdown implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final long applicantId;
    private final long jobId;
    private final double total;
    private final double skillCoverage;
    private final double tfidfCosine;
    private final double preferenceBoost;
    private final double locationFit;
    private final double stipendFit;
    private final List<Long> matchedSkillIds;
    private final List<Long> missingRequiredSkillIds;
    private final List<Long> missingOptionalSkillIds;
    /** True if this card was inserted for exploration (cold start), not because it ranked high. */
    private boolean exploration;

    public ScoreBreakdown(long applicantId, long jobId, double total, double skillCoverage, double tfidfCosine,
                          double preferenceBoost, double locationFit, double stipendFit,
                          List<Long> matchedSkillIds, List<Long> missingRequiredSkillIds,
                          List<Long> missingOptionalSkillIds) {
        this.applicantId = applicantId;
        this.jobId = jobId;
        this.total = total;
        this.skillCoverage = skillCoverage;
        this.tfidfCosine = tfidfCosine;
        this.preferenceBoost = preferenceBoost;
        this.locationFit = locationFit;
        this.stipendFit = stipendFit;
        this.matchedSkillIds = List.copyOf(matchedSkillIds);
        this.missingRequiredSkillIds = List.copyOf(missingRequiredSkillIds);
        this.missingOptionalSkillIds = List.copyOf(missingOptionalSkillIds);
    }

    public long getApplicantId() { return applicantId; }
    public long getJobId() { return jobId; }
    public double getTotal() { return total; }
    public double getSkillCoverage() { return skillCoverage; }
    public double getTfidfCosine() { return tfidfCosine; }
    public double getPreferenceBoost() { return preferenceBoost; }
    public double getLocationFit() { return locationFit; }
    public double getStipendFit() { return stipendFit; }
    public List<Long> getMatchedSkillIds() { return matchedSkillIds; }
    public List<Long> getMissingRequiredSkillIds() { return missingRequiredSkillIds; }
    public List<Long> getMissingOptionalSkillIds() { return missingOptionalSkillIds; }
    public boolean isExploration() { return exploration; }
    public void setExploration(boolean exploration) { this.exploration = exploration; }

    // 0..100 values for the JSP score bars.
    public int getTotalPercent() { return pct(total); }
    public int getSkillCoveragePercent() { return pct(skillCoverage); }
    public int getTfidfPercent() { return pct(tfidfCosine); }
    public int getPreferencePercent() { return pct(preferenceBoost); }
    public int getLocationPercent() { return pct(locationFit); }
    public int getStipendPercent() { return pct(stipendFit); }

    private static int pct(double x) {
        return (int) Math.round(x * 100);
    }
}
