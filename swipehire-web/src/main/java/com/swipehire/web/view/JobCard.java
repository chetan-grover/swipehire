package com.swipehire.web.view;

import com.swipehire.common.dto.ScoreBreakdown;
import com.swipehire.web.entity.Job;

import java.util.List;

/**
 * One card in the applicant's deck: the job plus the explanation of its score.
 * Plain getters (not a record) because JSP EL on Tomcat 10 only understands getX() methods.
 */
public class JobCard {

    private final Job job;
    private final ScoreBreakdown score;
    private final List<String> matchedSkills;
    private final List<String> missingRequired;
    private final List<String> missingOptional;
    /** % of decided applications the hirer acted on within 7 days; null for a new hirer. */
    private final Integer responsiveness;
    /** The hirer already liked this applicant from the Discover tab. */
    private final boolean companyInterested;

    public JobCard(Job job, ScoreBreakdown score, List<String> matchedSkills, List<String> missingRequired,
                   List<String> missingOptional, Integer responsiveness, boolean companyInterested) {
        this.job = job;
        this.score = score;
        this.matchedSkills = matchedSkills;
        this.missingRequired = missingRequired;
        this.missingOptional = missingOptional;
        this.responsiveness = responsiveness;
        this.companyInterested = companyInterested;
    }

    public Job getJob() { return job; }
    public ScoreBreakdown getScore() { return score; }
    public List<String> getMatchedSkills() { return matchedSkills; }
    public List<String> getMissingRequired() { return missingRequired; }
    public List<String> getMissingOptional() { return missingOptional; }
    public Integer getResponsiveness() { return responsiveness; }
    public boolean isCompanyInterested() { return companyInterested; }
}
