package com.swipehire.web.service;

import com.swipehire.common.dto.ApplicantData;
import com.swipehire.common.dto.JobData;
import com.swipehire.common.dto.ScoreBreakdown;
import com.swipehire.web.dao.AnalyticsDao;
import com.swipehire.web.dao.ScoringDataDao;
import com.swipehire.web.dao.SwipeDao;
import com.swipehire.web.entity.Job;
import com.swipehire.web.repository.JobRepository;
import com.swipehire.web.view.JobCard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Builds the applicant's ranked deck.
 * <p>
 * Prefetching: right after a swipe the controller calls {@link #refreshInBackground}, which
 * starts re-ranking on a worker thread (CompletableFuture). When the next page loads a moment
 * later the ranking is usually already done, so the UI stays fast even though ranking involves
 * an RMI round-trip. The futures live in a ConcurrentHashMap keyed by applicant id.
 */
@Service
public class DeckService {

    private static final Logger log = LoggerFactory.getLogger(DeckService.class);
    static final int DECK_SIZE = 30;

    private final Map<Long, CompletableFuture<List<ScoreBreakdown>>> prefetched = new ConcurrentHashMap<>();
    private final ScoringDataDao scoringDao;
    private final SwipeDao swipeDao;
    private final AnalyticsDao analyticsDao;
    private final JobRepository jobs;
    private final EngineClient engine;
    private final SkillCatalog catalog;
    private final ExecutorService taskPool;

    public DeckService(ScoringDataDao scoringDao, SwipeDao swipeDao, AnalyticsDao analyticsDao, JobRepository jobs,
                       EngineClient engine, SkillCatalog catalog, ExecutorService taskPool) {
        this.scoringDao = scoringDao;
        this.swipeDao = swipeDao;
        this.analyticsDao = analyticsDao;
        this.jobs = jobs;
        this.engine = engine;
        this.catalog = catalog;
        this.taskPool = taskPool;
    }

    /** Loads candidates, ranks them (RMI engine or fallback) and stores the score components. */
    List<ScoreBreakdown> rank(long applicantId) {
        ApplicantData applicant = scoringDao.loadApplicant(applicantId).orElse(null);
        if (applicant == null) {
            return List.of();
        }
        List<JobData> candidates = scoringDao.loadCandidateJobs(applicantId);
        List<ScoreBreakdown> ranked = engine.rankJobs(applicant, candidates, DECK_SIZE);
        scoringDao.saveScores(ranked);
        return ranked;
    }

    public void refreshInBackground(long applicantId) {
        prefetched.put(applicantId, CompletableFuture.supplyAsync(() -> rank(applicantId), taskPool));
    }

    /** Returns up to {@code max} cards, best first, skipping anything swiped since the ranking was made. */
    public List<JobCard> cards(long applicantId, int max) {
        List<ScoreBreakdown> ranked;
        CompletableFuture<List<ScoreBreakdown>> future = prefetched.get(applicantId);
        try {
            ranked = future != null ? future.join() : null;
        } catch (RuntimeException e) {
            log.warn("Prefetch failed for applicant {}: {}", applicantId, e.getMessage());
            ranked = null;
        }
        if (ranked == null) {
            ranked = rank(applicantId);
            prefetched.put(applicantId, CompletableFuture.completedFuture(ranked));
        }
        Set<Long> swiped = swipeDao.swipedJobIds(applicantId);
        List<ScoreBreakdown> visible = ranked.stream().filter(s -> !swiped.contains(s.getJobId())).limit(max).toList();
        return toCards(applicantId, visible);
    }

    public int remaining(long applicantId) {
        CompletableFuture<List<ScoreBreakdown>> f = prefetched.get(applicantId);
        if (f == null || !f.isDone()) {
            return -1;
        }
        Set<Long> swiped = swipeDao.swipedJobIds(applicantId);
        return (int) f.join().stream().filter(s -> !swiped.contains(s.getJobId())).count();
    }

    private List<JobCard> toCards(long applicantId, List<ScoreBreakdown> scores) {
        if (scores.isEmpty()) {
            return List.of();
        }
        Map<Long, Job> byId = jobs.findAllById(scores.stream().map(ScoreBreakdown::getJobId).toList()).stream()
                .collect(Collectors.toMap(Job::getId, Function.identity()));
        Map<Long, Integer> responsiveness = analyticsDao.responsiveness(
                byId.values().stream().map(Job::getPostedBy).collect(Collectors.toSet()));
        Set<Long> interested = swipeDao.jobsWhereHirerLiked(applicantId);
        return scores.stream()
                .filter(s -> byId.containsKey(s.getJobId()))
                .map(s -> {
                    Job j = byId.get(s.getJobId());
                    return new JobCard(j, s, catalog.names(s.getMatchedSkillIds()),
                            catalog.names(s.getMissingRequiredSkillIds()), catalog.names(s.getMissingOptionalSkillIds()),
                            responsiveness.get(j.getPostedBy()), interested.contains(j.getId()));
                })
                .toList();
    }
}
