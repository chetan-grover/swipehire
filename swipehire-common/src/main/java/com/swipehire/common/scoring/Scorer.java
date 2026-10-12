package com.swipehire.common.scoring;

import com.swipehire.common.dto.ApplicantData;
import com.swipehire.common.dto.JobData;
import com.swipehire.common.dto.ScoreBreakdown;
import com.swipehire.common.enums.WorkMode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The explainable score:
 * <pre>
 * total = 0.45 skillCoverage + 0.20 tfidfCosine + 0.15 preferenceBoost + 0.10 locationFit + 0.10 stipendFit
 * </pre>
 * Every component is in [0, 1], so the total is too. Stateless, therefore thread-safe.
 */
public final class Scorer {

    public static final double W_SKILL = 0.45;
    public static final double W_TFIDF = 0.20;
    public static final double W_PREFERENCE = 0.15;
    public static final double W_LOCATION = 0.10;
    public static final double W_STIPEND = 0.10;

    /** Proficiency at or above this (out of 5) counts as fully covering a skill. */
    static final int FULL_PROFICIENCY = 3;

    public ScoreBreakdown score(ApplicantData a, JobData j, double tfidfCosine) {
        double skill = skillCoverage(a, j);
        double pref = preferenceBoost(a, j);
        double loc = locationFit(a, j);
        double stipend = stipendFit(a, j);
        double tfidf = clamp01(tfidfCosine);
        double total = W_SKILL * skill + W_TFIDF * tfidf + W_PREFERENCE * pref + W_LOCATION * loc + W_STIPEND * stipend;

        // Explanations: which job skills the applicant has / lacks, heaviest first.
        Comparator<Long> byWeightDesc = Comparator.comparing((Long id) -> j.getSkillWeights().get(id)).reversed()
                .thenComparing(Comparator.naturalOrder());
        List<Long> matched = new ArrayList<>();
        List<Long> missingRequired = new ArrayList<>();
        List<Long> missingOptional = new ArrayList<>();
        for (Long skillId : j.getSkillWeights().keySet()) {
            if (a.getSkillProficiency().containsKey(skillId)) {
                matched.add(skillId);
            } else if (j.getRequiredSkillIds().contains(skillId)) {
                missingRequired.add(skillId);
            } else {
                missingOptional.add(skillId);
            }
        }
        matched.sort(byWeightDesc);
        missingRequired.sort(byWeightDesc);
        missingOptional.sort(byWeightDesc);

        return new ScoreBreakdown(a.getId(), j.getId(), total, skill, tfidf, pref, loc, stipend,
                matched, missingRequired, missingOptional);
    }

    /**
     * Weighted share of the job's required skills the applicant covers. Low proficiency counts
     * partially (proficiency 1 of 3 = one third). If a job marks nothing required, all its skills count.
     */
    static double skillCoverage(ApplicantData a, JobData j) {
        Map<Long, Integer> weights = j.getSkillWeights();
        if (weights.isEmpty()) {
            return 0;
        }
        Set<Long> considered = j.getRequiredSkillIds().isEmpty() ? weights.keySet() : j.getRequiredSkillIds();
        double totalWeight = 0;
        double covered = 0;
        for (Long skillId : considered) {
            int w = weights.getOrDefault(skillId, 1);
            totalWeight += w;
            Integer prof = a.getSkillProficiency().get(skillId);
            if (prof != null) {
                covered += w * Math.min(1.0, prof / (double) FULL_PROFICIENCY);
            }
        }
        return totalWeight == 0 ? 0 : covered / totalWeight;
    }

    /**
     * Average learned affinity (-1..1) over the job's skills, mapped to 0..1.
     * With no swipe history every affinity is 0, so this is a neutral 0.5 for all jobs.
     */
    static double preferenceBoost(ApplicantData a, JobData j) {
        if (j.getSkillWeights().isEmpty()) {
            return 0.5;
        }
        double sum = 0;
        for (Long skillId : j.getSkillWeights().keySet()) {
            sum += a.getSkillAffinity().getOrDefault(skillId, 0.0);
        }
        double avg = sum / j.getSkillWeights().size();
        return clamp01((avg + 1) / 2);
    }

    static double locationFit(ApplicantData a, JobData j) {
        if (j.getWorkMode() == WorkMode.REMOTE) {
            return 1;
        }
        if (a.getPrefWorkMode() == WorkMode.REMOTE) {
            return 0;
        }
        boolean cityOk = a.getPreferredCities().isEmpty()
                || (j.getCity() != null && a.getPreferredCities().contains(j.getCity().trim().toLowerCase()));
        if (cityOk) {
            return 1;
        }
        // Hybrid in another city is a partial fit (fewer office days to travel for).
        return j.getWorkMode() == WorkMode.HYBRID ? 0.5 : 0;
    }

    /** 1 if the job can pay the applicant's minimum; falls linearly to 0 at half the minimum. */
    static double stipendFit(ApplicantData a, JobData j) {
        int min = a.getMinStipend();
        if (min <= 0 || j.getStipendMax() >= min) {
            return 1;
        }
        double ratio = j.getStipendMax() / (double) min;
        return clamp01((ratio - 0.5) / 0.5);
    }

    static double clamp01(double x) {
        return Math.max(0, Math.min(1, x));
    }
}
