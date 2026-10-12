package com.swipehire.common.scoring;

import com.swipehire.common.dto.ApplicantData;
import com.swipehire.common.dto.JobData;
import com.swipehire.common.enums.JobType;
import com.swipehire.common.enums.WorkMode;

import java.util.Optional;

/**
 * Rules that remove a card completely (applied before scoring). Job status, deadline and
 * "already swiped" are filtered in SQL when candidates are loaded; these are the profile rules.
 */
public final class HardFilters {

    private HardFilters() {
    }

    public static boolean passes(ApplicantData a, JobData j) {
        return firstFailure(a, j).isEmpty();
    }

    /** The reason the pair is filtered out, or empty if it passes. */
    public static Optional<String> firstFailure(ApplicantData a, JobData j) {
        if (a.getPrefJobType() != JobType.ANY && a.getPrefJobType() != j.getJobType()) {
            return Optional.of("job type");
        }
        // Someone who only wants remote work should never see office jobs.
        if (a.getPrefWorkMode() == WorkMode.REMOTE && j.getWorkMode() != WorkMode.REMOTE) {
            return Optional.of("work mode");
        }
        if (j.getMinGradYear() != null || j.getMaxGradYear() != null) {
            Integer g = a.getGradYear();
            if (g == null) {
                return Optional.of("graduation year unknown");
            }
            if ((j.getMinGradYear() != null && g < j.getMinGradYear())
                    || (j.getMaxGradYear() != null && g > j.getMaxGradYear())) {
                return Optional.of("graduation year");
            }
        }
        if (j.isVerifiedOnly() && !a.isCollegeVerified()) {
            return Optional.of("verified students only");
        }
        if (!j.getAllowedCollegeIds().isEmpty()
                && (!a.isCollegeVerified() || a.getCollegeId() == null
                || !j.getAllowedCollegeIds().contains(a.getCollegeId()))) {
            return Optional.of("college not allowed");
        }
        return Optional.empty();
    }
}
