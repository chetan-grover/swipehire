package com.swipehire.common.scoring;

import com.swipehire.common.dto.ApplicantData;
import com.swipehire.common.dto.JobData;
import com.swipehire.common.dto.ScoreBreakdown;
import com.swipehire.common.enums.WorkMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.swipehire.common.scoring.Fixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class RankerTest {

    private final ExecutorService pool = Executors.newFixedThreadPool(4);
    private final Ranker ranker = new Ranker(pool, 4, new Random(42));

    @AfterEach
    void shutdown() {
        pool.shutdownNow();
    }

    @Test
    void betterSkillMatchRanksHigher() {
        ApplicantData a = applicant(1, Map.of(JAVA, 4, SPRING, 4, SQL, 4));
        JobData javaJob = javaJob(1);
        JobData reactJob = javaJob(2);
        reactJob.setTitle("Frontend Intern");
        reactJob.setText("React JavaScript user interfaces");
        reactJob.setSkillWeights(Map.of(REACT, 5));
        reactJob.setRequiredSkillIds(Set.of(REACT));

        List<ScoreBreakdown> deck = ranker.rankJobs(a, List.of(reactJob, javaJob), 10);
        assertEquals(List.of(1L, 2L), deck.stream().map(ScoreBreakdown::getJobId).toList());
        assertTrue(deck.get(0).getTfidfCosine() > deck.get(1).getTfidfCosine(), "text similarity should agree");
    }

    @Test
    void hardFilteredJobsNeverAppear() {
        ApplicantData a = applicant(1, Map.of(JAVA, 4));
        a.setPrefWorkMode(WorkMode.REMOTE);
        assertTrue(ranker.rankJobs(a, List.of(javaJob(1)), 10).isEmpty());
    }

    @Test
    void parallelScoringReturnsEveryEligibleJobExactlyOnce() {
        ApplicantData a = applicant(1, Map.of(JAVA, 3));
        List<JobData> jobs = new ArrayList<>();
        for (long id = 1; id <= 37; id++) {
            jobs.add(javaJob(id));
        }
        List<ScoreBreakdown> deck = ranker.rankJobs(a, jobs, 100);
        Set<Long> ids = new HashSet<>(deck.stream().map(ScoreBreakdown::getJobId).toList());
        assertEquals(37, deck.size());
        assertEquals(37, ids.size());
    }

    @Test
    void everyTenthCardIsAnExplorationPickFromTheLowerHalf() {
        List<ScoreBreakdown> sorted = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            double total = 1.0 - i / 100.0;   // strictly descending
            sorted.add(new ScoreBreakdown(1, i, total, 0, 0, 0, 0, 0, List.of(), List.of(), List.of()));
        }
        List<ScoreBreakdown> deck = ranker.withExploration(sorted, 20);
        assertEquals(20, deck.size());
        for (int slot = 0; slot < deck.size(); slot++) {
            boolean shouldExplore = (slot + 1) % 10 == 0;
            assertEquals(shouldExplore, deck.get(slot).isExploration(), "slot " + slot);
        }
        // Slot 9 explores among the 21 cards still left: it must come from their lower half.
        assertTrue(deck.get(9).getJobId() >= 9 + 21 / 2);
        // Non-exploration cards keep strictly descending order.
        assertEquals(List.of(0L, 1L, 2L), deck.subList(0, 3).stream().map(ScoreBreakdown::getJobId).toList());
    }

    @Test
    void rankApplicantsOrdersCandidatesForAJob() {
        ApplicantData strong = applicant(1, Map.of(JAVA, 5, SPRING, 5, SQL, 5));
        ApplicantData weak = applicant(2, Map.of(GIT, 2));
        weak.setText("photography and music");
        List<ScoreBreakdown> ranked = ranker.rankApplicants(javaJob(1), List.of(weak, strong));
        assertEquals(List.of(1L, 2L), ranked.stream().map(ScoreBreakdown::getApplicantId).toList());
    }
}
