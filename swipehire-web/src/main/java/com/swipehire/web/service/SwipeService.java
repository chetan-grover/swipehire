package com.swipehire.web.service;

import com.swipehire.common.enums.HirerSwipeSource;
import com.swipehire.common.enums.SwipeDirection;
import com.swipehire.web.config.AppTime;
import com.swipehire.web.dao.QuotaDao;
import com.swipehire.web.dao.SwipeDao;
import com.swipehire.web.entity.Job;
import com.swipehire.web.entity.JobSkill;
import com.swipehire.web.repository.JobRepository;
import com.swipehire.web.support.BusinessException;
import com.swipehire.web.support.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Both sides' swipes. Each swipe is one transaction:
 * lock applicant row -> check quota -> insert swipe -> learn preferences -> detect match.
 * Any BusinessException rolls the whole thing back (including the quota increment).
 */
@Service
public class SwipeService {

    public static final int PITCH_MAX = 140;
    public static final Duration UNDO_WINDOW = Duration.ofSeconds(10);

    private final SwipeDao swipeDao;
    private final QuotaDao quotaDao;
    private final JobRepository jobs;
    private final JobService jobService;
    private final MatchService matchService;
    private final EngineClient engine;

    public SwipeService(SwipeDao swipeDao, QuotaDao quotaDao, JobRepository jobs, JobService jobService,
                        MatchService matchService, EngineClient engine) {
        this.swipeDao = swipeDao;
        this.quotaDao = quotaDao;
        this.jobs = jobs;
        this.jobService = jobService;
        this.matchService = matchService;
        this.engine = engine;
    }

    /** @return true if this swipe created a match */
    @Transactional
    public boolean applicantSwipe(long applicantId, long jobId, SwipeDirection direction, String pitch) {
        Job job = jobs.findById(jobId).orElseThrow(() -> new NotFoundException("Job not found"));
        if (!job.isOpen()) {
            throw new BusinessException("This job is no longer accepting applicants");
        }
        String cleanPitch = null;
        if (direction == SwipeDirection.SUPER) {
            cleanPitch = pitch == null ? "" : pitch.trim();
            if (cleanPitch.isEmpty() || cleanPitch.length() > PITCH_MAX) {
                throw new BusinessException("A super-swipe needs a pitch of 1-" + PITCH_MAX + " characters");
            }
        }

        swipeDao.lockApplicant(applicantId);

        if (direction != SwipeDirection.LEFT
                && !quotaDao.tryConsume(applicantId, AppTime.today(), direction == SwipeDirection.SUPER)) {
            throw new BusinessException(direction == SwipeDirection.SUPER
                    ? "You've used all " + QuotaDao.SUPER_LIMIT + " super-swipes for today"
                    : "You've used all " + QuotaDao.RIGHT_LIMIT + " right-swipes for today. Come back tomorrow!");
        }
        if (!swipeDao.insertApplicantSwipe(applicantId, jobId, direction, cleanPitch)) {
            throw new BusinessException("You already swiped on this job");
        }

        learn(applicantId, job, direction, false);

        if (direction.isPositive()
                && swipeDao.findHirerDirection(applicantId, jobId).map(d -> d == SwipeDirection.RIGHT).orElse(false)) {
            return matchService.createMatch(applicantId, jobId);
        }
        return false;
    }

    /** Undo the applicant's last swipe within 10 seconds, unless it already produced a match. */
    @Transactional
    public long undoLast(long applicantId) {
        swipeDao.lockApplicant(applicantId);
        SwipeDao.LastSwipe last = swipeDao.findLastSwipe(applicantId)
                .orElseThrow(() -> new BusinessException("Nothing to undo"));
        if (last.createdAt().isBefore(AppTime.now().minus(UNDO_WINDOW))) {
            throw new BusinessException("Undo is only possible within " + UNDO_WINDOW.toSeconds() + " seconds");
        }
        if (swipeDao.matchExists(applicantId, last.jobId())) {
            throw new BusinessException("That swipe already created a match, so it can't be undone");
        }
        swipeDao.deleteApplicantSwipe(last.id());
        if (last.direction() != SwipeDirection.LEFT) {
            quotaDao.refund(applicantId, last.createdAt().toLocalDate(), last.direction() == SwipeDirection.SUPER);
        }
        jobs.findById(last.jobId()).ifPresent(job -> learn(applicantId, job, last.direction(), true));
        return last.jobId();
    }

    /** @return true if this swipe created a match */
    @Transactional
    public boolean hirerSwipe(long hirerId, long jobId, long applicantId, SwipeDirection direction, HirerSwipeSource source) {
        if (direction == SwipeDirection.SUPER) {
            throw new BusinessException("Super-swipes are for applicants only");
        }
        jobService.requireOwned(jobId, hirerId);
        swipeDao.lockApplicant(applicantId);
        if (!swipeDao.insertHirerSwipe(hirerId, applicantId, jobId, direction, source)) {
            throw new BusinessException("You already reviewed this applicant for this job");
        }
        if (direction == SwipeDirection.RIGHT
                && swipeDao.findApplicantDirection(applicantId, jobId).map(SwipeDirection::isPositive).orElse(false)) {
            return matchService.createMatch(applicantId, jobId);
        }
        return false;
    }

    /** Nudges the applicant's skill affinities (computed by the RMI engine) and saves them in one batch. */
    private void learn(long applicantId, Job job, SwipeDirection direction, boolean undo) {
        List<Long> skillIds = job.getSkills().stream().map(JobSkill::getSkill).map(s -> s.getId()).toList();
        Map<Long, Double> updated = engine.updatePreferences(swipeDao.loadAffinity(applicantId), skillIds, direction, undo);
        swipeDao.upsertAffinities(applicantId, updated);
    }

    public int[] quotaUsage(long applicantId) {
        return quotaDao.usage(applicantId, AppTime.today());
    }
}
