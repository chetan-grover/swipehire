package com.swipehire.web.view;

import com.swipehire.common.dto.ScoreBreakdown;
import com.swipehire.web.entity.ApplicantProfile;
import com.swipehire.web.entity.ResumeItem;

import java.util.List;

/** One applicant card in a hirer's queue or Discover tab. */
public class CandidateCard {

    private final ApplicantProfile profile;
    private final ScoreBreakdown score;
    private final List<String> matchedSkills;
    private final List<String> missingRequired;
    private final List<ResumeItem> topItems;
    private final boolean superSwipe;
    private final String pitch;

    public CandidateCard(ApplicantProfile profile, ScoreBreakdown score, List<String> matchedSkills,
                         List<String> missingRequired, List<ResumeItem> topItems, boolean superSwipe, String pitch) {
        this.profile = profile;
        this.score = score;
        this.matchedSkills = matchedSkills;
        this.missingRequired = missingRequired;
        this.topItems = topItems;
        this.superSwipe = superSwipe;
        this.pitch = pitch;
    }

    public ApplicantProfile getProfile() { return profile; }
    public ScoreBreakdown getScore() { return score; }
    public List<String> getMatchedSkills() { return matchedSkills; }
    public List<String> getMissingRequired() { return missingRequired; }
    public List<ResumeItem> getTopItems() { return topItems; }
    public boolean isSuperSwipe() { return superSwipe; }
    public String getPitch() { return pitch; }
}
