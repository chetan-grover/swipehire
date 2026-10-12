package com.swipehire.common.scoring;

import com.swipehire.common.dto.ApplicantData;
import com.swipehire.common.dto.JobData;
import com.swipehire.common.dto.ScoreBreakdown;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.IntFunction;

/**
 * Ranks cards for one side. Scoring is split into chunks that run in parallel on the given
 * thread pool (invokeAll waits for all of them), then the results are merged and sorted.
 * The same class runs inside the RMI engine and in the web app's fallback mode.
 */
public class Ranker {

    /** Every 10th card is an exploration card (10% of the deck). */
    public static final int EXPLORATION_EVERY = 10;

    private final ExecutorService pool;
    private final int parallelism;
    private final Random random;
    private final Scorer scorer = new Scorer();

    public Ranker(ExecutorService pool, int parallelism, Random random) {
        this.pool = pool;
        this.parallelism = Math.max(1, parallelism);
        this.random = random;
    }

    /** Applicant deck: filter, score every job, sort, then mix in exploration cards. */
    public List<ScoreBreakdown> rankJobs(ApplicantData applicant, List<JobData> jobs, int limit) {
        List<JobData> eligible = jobs.stream().filter(j -> HardFilters.passes(applicant, j)).toList();
        if (eligible.isEmpty()) {
            return List.of();
        }
        Map<String, Integer> applicantTf = TextAnalyzer.termFrequencies(applicant.getText());
        List<Map<String, Integer>> jobTfs = eligible.stream().map(j -> TextAnalyzer.termFrequencies(j.getText())).toList();

        List<Map<String, Integer>> corpus = new ArrayList<>(jobTfs);
        corpus.add(applicantTf);
        TfIdfModel model = new TfIdfModel(corpus);
        Map<String, Double> applicantVec = model.vectorize(applicantTf);

        List<ScoreBreakdown> scored = scoreInParallel(eligible.size(), i ->
                scorer.score(applicant, eligible.get(i), TfIdfModel.cosine(applicantVec, model.vectorize(jobTfs.get(i)))));
        scored.sort(Comparator.comparingDouble(ScoreBreakdown::getTotal).reversed());
        return withExploration(scored, limit);
    }

    /** Hirer queue / Discover: filter and rank applicants for one job. No exploration on this side. */
    public List<ScoreBreakdown> rankApplicants(JobData job, List<ApplicantData> applicants) {
        List<ApplicantData> eligible = applicants.stream().filter(a -> HardFilters.passes(a, job)).toList();
        if (eligible.isEmpty()) {
            return List.of();
        }
        Map<String, Integer> jobTf = TextAnalyzer.termFrequencies(job.getText());
        List<Map<String, Integer>> appTfs = eligible.stream().map(a -> TextAnalyzer.termFrequencies(a.getText())).toList();

        List<Map<String, Integer>> corpus = new ArrayList<>(appTfs);
        corpus.add(jobTf);
        TfIdfModel model = new TfIdfModel(corpus);
        Map<String, Double> jobVec = model.vectorize(jobTf);

        List<ScoreBreakdown> scored = scoreInParallel(eligible.size(), i ->
                scorer.score(eligible.get(i), job, TfIdfModel.cosine(model.vectorize(appTfs.get(i)), jobVec)));
        scored.sort(Comparator.comparingDouble(ScoreBreakdown::getTotal).reversed());
        return scored;
    }

    private List<ScoreBreakdown> scoreInParallel(int n, IntFunction<ScoreBreakdown> scoreAt) {
        int chunkSize = (int) Math.ceil(n / (double) parallelism);
        List<Callable<List<ScoreBreakdown>>> tasks = new ArrayList<>();
        for (int start = 0; start < n; start += chunkSize) {
            int from = start;
            int to = Math.min(n, start + chunkSize);
            tasks.add(() -> {
                List<ScoreBreakdown> part = new ArrayList<>(to - from);
                for (int i = from; i < to; i++) {
                    part.add(scoreAt.apply(i));
                }
                return part;
            });
        }
        try {
            List<ScoreBreakdown> all = new ArrayList<>(n);
            for (Future<List<ScoreBreakdown>> f : pool.invokeAll(tasks)) {
                all.addAll(f.get());
            }
            return all;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Ranking interrupted", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Ranking failed", e.getCause());
        }
    }

    /**
     * Builds the deck: normally the next best card, but every 10th slot takes a random card from
     * the lower half of what's left. This gives new or unusual jobs a chance to be seen (cold
     * start) and stops the deck from only ever showing more of the same.
     */
    List<ScoreBreakdown> withExploration(List<ScoreBreakdown> sorted, int limit) {
        List<ScoreBreakdown> remaining = new ArrayList<>(sorted);
        List<ScoreBreakdown> deck = new ArrayList<>();
        int size = Math.min(limit, sorted.size());
        for (int slot = 0; slot < size; slot++) {
            boolean exploreSlot = (slot + 1) % EXPLORATION_EVERY == 0 && remaining.size() > 1;
            ScoreBreakdown pick;
            if (exploreSlot) {
                int lowerHalfStart = remaining.size() / 2;
                int index = lowerHalfStart + random.nextInt(remaining.size() - lowerHalfStart);
                pick = remaining.remove(index);
                pick.setExploration(true);
            } else {
                pick = remaining.remove(0);
            }
            deck.add(pick);
        }
        return deck;
    }
}
