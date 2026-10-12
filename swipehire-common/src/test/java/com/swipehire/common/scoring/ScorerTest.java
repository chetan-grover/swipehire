package com.swipehire.common.scoring;

import com.swipehire.common.dto.ApplicantData;
import com.swipehire.common.dto.JobData;
import com.swipehire.common.dto.ScoreBreakdown;
import com.swipehire.common.enums.WorkMode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.swipehire.common.scoring.Fixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class ScorerTest {

    private final Scorer scorer = new Scorer();

    @Test
    void weightsSumToOne() {
        assertEquals(1.0, Scorer.W_SKILL + Scorer.W_TFIDF + Scorer.W_PREFERENCE + Scorer.W_LOCATION + Scorer.W_STIPEND, 1e-9);
    }

    @Test
    void fullRequiredCoverageGivesSkillScoreOne() {
        ApplicantData a = applicant(1, Map.of(JAVA, 4, SPRING, 3, SQL, 5));
        assertEquals(1.0, Scorer.skillCoverage(a, javaJob(1)), 1e-9);
    }

    @Test
    void coverageIsWeightedAndProficiencyAdjusted() {
        // Has Java (w5) at full proficiency and Spring (w4) at proficiency 1 => (5 + 4/3) / 12
        ApplicantData a = applicant(1, Map.of(JAVA, 5, SPRING, 1));
        assertEquals((5 + 4.0 / 3) / 12, Scorer.skillCoverage(a, javaJob(1)), 1e-9);
    }

    @Test
    void optionalSkillsDoNotCountTowardsCoverage() {
        ApplicantData a = applicant(1, Map.of(GIT, 5));
        assertEquals(0.0, Scorer.skillCoverage(a, javaJob(1)), 1e-9);
    }

    @Test
    void explanationListsMatchedAndMissingSkillsHeaviestFirst() {
        ApplicantData a = applicant(1, Map.of(SQL, 3, JAVA, 3));
        ScoreBreakdown s = scorer.score(a, javaJob(1), 0.5);
        assertEquals(List.of(JAVA, SQL), s.getMatchedSkillIds());
        assertEquals(List.of(SPRING), s.getMissingRequiredSkillIds());
        assertEquals(List.of(GIT), s.getMissingOptionalSkillIds());
    }

    @Test
    void totalIsTheWeightedSumOfComponents() {
        ApplicantData a = applicant(1, Map.of(JAVA, 3, SPRING, 3, SQL, 3));
        ScoreBreakdown s = scorer.score(a, javaJob(1), 0.4);
        double expected = 0.45 * 1 + 0.20 * 0.4 + 0.15 * 0.5 + 0.10 * 1 + 0.10 * 1;
        assertEquals(expected, s.getTotal(), 1e-9);
    }

    @Test
    void preferenceBoostIsNeutralWithoutHistoryAndRisesWithAffinity() {
        ApplicantData a = applicant(1, Map.of());
        JobData j = javaJob(1);
        assertEquals(0.5, Scorer.preferenceBoost(a, j), 1e-9);
        a.setSkillAffinity(Map.of(JAVA, 1.0, SPRING, 1.0, SQL, 1.0, GIT, 1.0));
        assertEquals(1.0, Scorer.preferenceBoost(a, j), 1e-9);
    }

    @Test
    void locationFitRules() {
        ApplicantData a = applicant(1, Map.of());
        JobData j = javaJob(1);
        assertEquals(1.0, Scorer.locationFit(a, j), "preferred city");
        j.setCity("Mumbai");
        assertEquals(0.0, Scorer.locationFit(a, j), "onsite elsewhere");
        j.setWorkMode(WorkMode.HYBRID);
        assertEquals(0.5, Scorer.locationFit(a, j), "hybrid elsewhere");
        j.setWorkMode(WorkMode.REMOTE);
        assertEquals(1.0, Scorer.locationFit(a, j), "remote");
    }

    @Test
    void stipendFitFallsLinearlyToZeroAtHalfTheMinimum() {
        ApplicantData a = applicant(1, Map.of());
        JobData j = javaJob(1);           // pays up to 30000
        a.setMinStipend(30000);
        assertEquals(1.0, Scorer.stipendFit(a, j), 1e-9);
        a.setMinStipend(40000);           // ratio 0.75 -> 0.5
        assertEquals(0.5, Scorer.stipendFit(a, j), 1e-9);
        a.setMinStipend(60000);           // ratio 0.5 -> 0
        assertEquals(0.0, Scorer.stipendFit(a, j), 1e-9);
    }
}
