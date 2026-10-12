package com.swipehire.web.service;

import com.swipehire.common.dto.ApplicantData;
import com.swipehire.common.dto.JobData;
import com.swipehire.common.dto.ScoreBreakdown;
import com.swipehire.common.enums.SwipeDirection;
import com.swipehire.common.scoring.Scorer;
import com.swipehire.web.dao.AnalyticsDao;
import com.swipehire.web.dao.ScoringDataDao;
import com.swipehire.web.dao.SwipeDao;
import com.swipehire.web.entity.ApplicantProfile;
import com.swipehire.web.repository.ApplicantProfileRepository;
import com.swipehire.web.repository.ResumeItemRepository;
import com.swipehire.web.view.CandidateCard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The hirer's two review lists for a job:
 * <ul>
 *   <li>Queue: applicants who already liked the job (super-swipes pinned on top, then by score)</li>
 *   <li>Discover: opted-in applicants who haven't seen the job yet, ranked by score</li>
 * </ul>
 */
@Service
public class ReviewService {

    static final int DISCOVER_POOL = 200;

    private final JobService jobService;
    private final SwipeDao swipeDao;
    private final ScoringDataDao scoringDao;
    private final EngineClient engine;
    private final ApplicantProfileRepository profiles;
    private final ResumeItemRepository resumeItems;
    private final SkillCatalog catalog;
    private final AnalyticsDao analyticsDao;

    public ReviewService(JobService jobService, SwipeDao swipeDao, ScoringDataDao scoringDao, EngineClient engine,
                         ApplicantProfileRepository profiles, ResumeItemRepository resumeItems, SkillCatalog catalog,
                         AnalyticsDao analyticsDao) {
        this.analyticsDao = analyticsDao;
        this.jobService = jobService;
        this.swipeDao = swipeDao;
        this.scoringDao = scoringDao;
        this.engine = engine;
        this.profiles = profiles;
        this.resumeItems = resumeItems;
        this.catalog = catalog;
    }

    @Transactional  // not read-only: ranking also writes match_score_cache
    public List<CandidateCard> queue(long jobId, long hirerId) {
        jobService.requireOwned(jobId, hirerId);
        Map<Long, SwipeDao.QueueEntry> waiting = swipeDao.queueCandidates(jobId);
        List<ScoreBreakdown> ranked = rank(jobId, scoringDao.loadApplicants(waiting.keySet()));
        // Super-swipers first; inside each group the higher score first.
        List<ScoreBreakdown> ordered = new ArrayList<>(ranked);
        ordered.sort(Comparator.comparing((ScoreBreakdown s) -> waiting.get(s.getApplicantId()).direction() != SwipeDirection.SUPER)
                .thenComparing(Comparator.comparingDouble(ScoreBreakdown::getTotal).reversed()));
        return toCards(ordered, waiting);
    }

    @Transactional  // not read-only: ranking also writes match_score_cache
    public List<CandidateCard> discover(long jobId, long hirerId) {
        jobService.requireOwned(jobId, hirerId);
        List<Long> ids = scoringDao.discoverCandidateIds(jobId, DISCOVER_POOL);
        return toCards(rank(jobId, scoringDao.loadApplicants(ids)), Map.of());
    }

    /**
     * Live explanation for one pair (match pages): skill coverage, location, stipend and preference
     * are recomputed now; the TF-IDF part is taken from the stored score because IDF depends on
     * the whole set of documents that were ranked together.
     */
    public ScoreBreakdown explain(long applicantId, long jobId) {
        ApplicantData a = scoringDao.loadApplicant(applicantId).orElseThrow();
        JobData j = scoringDao.loadJob(jobId).orElseThrow();
        double[] stored = analyticsDao.storedScore(applicantId, jobId);
        return new Scorer().score(a, j, stored == null ? 0 : stored[2]);
    }

    private List<ScoreBreakdown> rank(long jobId, List<ApplicantData> applicants) {
        if (applicants.isEmpty()) {
            return List.of();
        }
        JobData job = scoringDao.loadJob(jobId).orElseThrow();
        List<ScoreBreakdown> ranked = engine.rankApplicants(job, applicants);
        scoringDao.saveScores(ranked);
        return ranked;
    }

    private List<CandidateCard> toCards(List<ScoreBreakdown> scores, Map<Long, SwipeDao.QueueEntry> entries) {
        Map<Long, ApplicantProfile> byId = profiles.findAllById(scores.stream().map(ScoreBreakdown::getApplicantId).toList())
                .stream().collect(Collectors.toMap(ApplicantProfile::getUserId, Function.identity()));
        List<CandidateCard> cards = new ArrayList<>();
        for (ScoreBreakdown s : scores) {
            ApplicantProfile p = byId.get(s.getApplicantId());
            if (p == null) {
                continue;
            }
            SwipeDao.QueueEntry e = entries.get(s.getApplicantId());
            cards.add(new CandidateCard(p, s, catalog.names(s.getMatchedSkillIds()),
                    catalog.names(s.getMissingRequiredSkillIds()),
                    resumeItems.findByApplicantIdOrderByDisplayOrderAsc(p.getUserId()).stream().limit(3).toList(),
                    e != null && e.direction() == SwipeDirection.SUPER, e == null ? null : e.pitch()));
        }
        return cards;
    }
}
