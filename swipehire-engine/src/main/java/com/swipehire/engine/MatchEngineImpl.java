package com.swipehire.engine;

import com.swipehire.common.dto.ApplicantData;
import com.swipehire.common.dto.JobData;
import com.swipehire.common.dto.ScoreBreakdown;
import com.swipehire.common.enums.SwipeDirection;
import com.swipehire.common.rmi.MatchEngine;
import com.swipehire.common.scoring.PreferenceUpdater;
import com.swipehire.common.scoring.Ranker;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Server-side implementation of the remote interface. Extending UnicastRemoteObject exports
 * this object so the RMI runtime can accept calls on it from other JVMs.
 * <p>
 * RMI may run calls from different clients on different threads at the same time, so this
 * class only holds thread-safe state: an ExecutorService, a Ranker (stateless apart from the
 * pool) and atomic counters.
 */
public class MatchEngineImpl extends UnicastRemoteObject implements MatchEngine {

    private final Instant startedAt = Instant.now();
    private final Ranker ranker;
    private final AtomicLong callCount = new AtomicLong();

    public MatchEngineImpl(ExecutorService rankingPool, int parallelism) throws RemoteException {
        super();
        this.ranker = new Ranker(rankingPool, parallelism, new Random());
    }

    @Override
    public String ping() {
        return "engine up since " + startedAt + ", calls served: " + callCount.get();
    }

    @Override
    public List<ScoreBreakdown> rankJobsForApplicant(ApplicantData applicant, List<JobData> candidateJobs, int limit) {
        long start = System.nanoTime();
        List<ScoreBreakdown> result = ranker.rankJobs(applicant, candidateJobs, limit);
        log("rankJobs applicant=" + applicant.getId() + " candidates=" + candidateJobs.size(), start);
        return result;
    }

    @Override
    public List<ScoreBreakdown> rankApplicantsForJob(JobData job, List<ApplicantData> candidates) {
        long start = System.nanoTime();
        List<ScoreBreakdown> result = ranker.rankApplicants(job, candidates);
        log("rankApplicants job=" + job.getId() + " candidates=" + candidates.size(), start);
        return result;
    }

    @Override
    public Map<Long, Double> updatePreferences(Map<Long, Double> currentAffinity, List<Long> jobSkillIds,
                                               SwipeDirection direction, boolean undo) {
        callCount.incrementAndGet();
        return PreferenceUpdater.apply(currentAffinity, jobSkillIds, direction, undo);
    }

    private void log(String what, long startNanos) {
        callCount.incrementAndGet();
        System.out.printf("[engine] %s on %s took %d ms%n", what, Thread.currentThread().getName(),
                (System.nanoTime() - startNanos) / 1_000_000);
    }
}
