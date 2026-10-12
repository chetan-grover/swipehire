package com.swipehire.common.scoring;

import com.swipehire.common.dto.ApplicantData;
import com.swipehire.common.dto.JobData;
import com.swipehire.common.enums.JobType;
import com.swipehire.common.enums.WorkMode;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static com.swipehire.common.scoring.Fixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class HardFiltersTest {

    @Test
    void remoteOnlyApplicantNeverSeesOfficeJobs() {
        ApplicantData a = applicant(1, Map.of());
        a.setPrefWorkMode(WorkMode.REMOTE);
        JobData j = javaJob(1);
        assertFalse(HardFilters.passes(a, j));
        j.setWorkMode(WorkMode.REMOTE);
        assertTrue(HardFilters.passes(a, j));
    }

    @Test
    void jobTypePreferenceIsEnforced() {
        ApplicantData a = applicant(1, Map.of());
        a.setPrefJobType(JobType.FULL_TIME);
        assertEquals("job type", HardFilters.firstFailure(a, javaJob(1)).orElseThrow());
    }

    @Test
    void graduationYearWindow() {
        ApplicantData a = applicant(1, Map.of());
        JobData j = javaJob(1);
        j.setMinGradYear(2025);
        j.setMaxGradYear(2026);
        assertFalse(HardFilters.passes(a, j));   // graduates 2027
        a.setGradYear(2026);
        assertTrue(HardFilters.passes(a, j));
        a.setGradYear(null);
        assertFalse(HardFilters.passes(a, j));
    }

    @Test
    void verifiedOnlyAndAllowedColleges() {
        ApplicantData a = applicant(1, Map.of());
        JobData j = javaJob(1);
        j.setVerifiedOnly(true);
        assertFalse(HardFilters.passes(a, j));
        a.setCollegeVerified(true);
        a.setCollegeId(7L);
        assertTrue(HardFilters.passes(a, j));
        j.setAllowedCollegeIds(Set.of(1L, 2L));
        assertEquals("college not allowed", HardFilters.firstFailure(a, j).orElseThrow());
        a.setCollegeId(2L);
        assertTrue(HardFilters.passes(a, j));
    }
}
