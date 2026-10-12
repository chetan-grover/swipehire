package com.swipehire.web;

import com.swipehire.common.enums.ApplicationStatus;
import com.swipehire.common.enums.HirerSwipeSource;
import com.swipehire.common.enums.SwipeDirection;
import com.swipehire.web.config.AppTime;
import com.swipehire.web.dao.QuotaDao;
import com.swipehire.web.service.PipelineService;
import com.swipehire.web.service.SwipeService;
import com.swipehire.web.support.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Concurrency tests against real MySQL: the daily quota cannot be exceeded and a mutual like
 * produces exactly one match even when both sides click at the same instant.
 */
@DatabaseTest
class QuotaAndMatchTest {

    /** Seed data: job 7 "DevOps Intern" (CloudNest, posted by hirer 4); applicants 101-110. */
    static final long JOB = 7, HIRER = 4;

    @Autowired SwipeService swipes;
    @Autowired QuotaDao quota;
    @Autowired PipelineService pipeline;
    @Autowired JdbcTemplate jdbc;   // only for test setup/assertions

    @BeforeEach
    void cleanJob() {
        jdbc.update("DELETE FROM job_match WHERE job_id = ?", JOB);
        jdbc.update("DELETE FROM applicant_swipe WHERE job_id = ?", JOB);
        jdbc.update("DELETE FROM hirer_swipe WHERE job_id = ?", JOB);
        jdbc.update("DELETE FROM daily_quota");
    }

    @Test
    void quotaAllowsExactly25RightSwipesEvenUnderConcurrency() throws Exception {
        long applicant = 110;
        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger granted = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            futures.add(pool.submit(() -> {
                start.await();
                if (quota.tryConsume(applicant, AppTime.today(), false)) {
                    granted.incrementAndGet();
                }
                return null;
            }));
        }
        start.countDown();                      // release all 40 threads at once
        for (Future<?> f : futures) {
            f.get();
        }
        pool.shutdown();
        assertEquals(QuotaDao.RIGHT_LIMIT, granted.get());
        assertEquals(QuotaDao.RIGHT_LIMIT, quota.usage(applicant, AppTime.today())[0]);
    }

    @Test
    void superSwipeQuotaIsThreePerDay() {
        for (int i = 0; i < QuotaDao.SUPER_LIMIT; i++) {
            assertTrue(quota.tryConsume(109, AppTime.today(), true));
        }
        assertFalse(quota.tryConsume(109, AppTime.today(), true));
    }

    @Test
    void simultaneousMutualLikesCreateExactlyOneMatch() throws Exception {
        // Applicants whose profile passes job 7's filters (it has none) - try several pairs.
        long[] applicants = {101, 102, 104, 105, 106};
        ExecutorService pool = Executors.newFixedThreadPool(2);
        for (long applicant : applicants) {
            CountDownLatch start = new CountDownLatch(1);
            Callable<Boolean> applicantSide = () -> {
                start.await();
                return swipes.applicantSwipe(applicant, JOB, SwipeDirection.RIGHT, null);
            };
            Callable<Boolean> hirerSide = () -> {
                start.await();
                return swipes.hirerSwipe(HIRER, JOB, applicant, SwipeDirection.RIGHT, HirerSwipeSource.DISCOVER);
            };
            Future<Boolean> a = pool.submit(applicantSide);
            Future<Boolean> h = pool.submit(hirerSide);
            start.countDown();
            boolean aMatched = a.get();
            boolean hMatched = h.get();

            assertTrue(aMatched ^ hMatched, "exactly one of the two swipes should report the match");
            assertEquals(1, count("SELECT COUNT(*) FROM job_match WHERE applicant_id = ? AND job_id = ?", applicant, JOB));
            assertEquals(1, count("""
                    SELECT COUNT(*) FROM application a JOIN job_match m ON m.id = a.match_id
                    WHERE m.applicant_id = ? AND m.job_id = ? AND a.status = 'MATCHED'""", applicant, JOB));
        }
        pool.shutdown();
    }

    @Test
    void leftSwipeNeverMatchesAndDuplicatesAreRejected() {
        assertFalse(swipes.hirerSwipe(HIRER, JOB, 107, SwipeDirection.RIGHT, HirerSwipeSource.DISCOVER));
        assertFalse(swipes.applicantSwipe(107, JOB, SwipeDirection.LEFT, null));
        assertEquals(0, count("SELECT COUNT(*) FROM job_match WHERE applicant_id = 107 AND job_id = ?", JOB));
        assertThrows(BusinessException.class, () -> swipes.applicantSwipe(107, JOB, SwipeDirection.RIGHT, null));
    }

    @Test
    void failedSwipeRollsBackTheQuota() {
        swipes.applicantSwipe(108, JOB, SwipeDirection.RIGHT, null);
        int before = quota.usage(108, AppTime.today())[0];
        // Same job again: the insert hits the UNIQUE key, the transaction rolls back, quota unchanged.
        assertThrows(BusinessException.class, () -> swipes.applicantSwipe(108, JOB, SwipeDirection.RIGHT, null));
        assertEquals(before, quota.usage(108, AppTime.today())[0]);
    }

    @Test
    void pipelineEnforcesTheStateMachineAndLogsHistory() {
        swipes.hirerSwipe(HIRER, JOB, 101, SwipeDirection.RIGHT, HirerSwipeSource.DISCOVER);
        swipes.applicantSwipe(101, JOB, SwipeDirection.RIGHT, null);
        long app = count("SELECT a.id FROM application a JOIN job_match m ON m.id = a.match_id WHERE m.applicant_id = 101 AND m.job_id = ?", JOB);

        assertThrows(BusinessException.class, () -> pipeline.hirerMove(HIRER, app, ApplicationStatus.SHORTLISTED, null),
                "hirer can't shortlist before the applicant applies");
        pipeline.apply(101, app);
        pipeline.hirerMove(HIRER, app, ApplicationStatus.SHORTLISTED, "nice");
        assertThrows(BusinessException.class, () -> pipeline.hirerMove(HIRER, app, ApplicationStatus.OFFER, null),
                "can't skip the interview stage");
        pipeline.hirerMove(HIRER, app, ApplicationStatus.INTERVIEW, null);
        pipeline.hirerMove(HIRER, app, ApplicationStatus.OFFER, null);
        assertEquals(5, count("SELECT COUNT(*) FROM application_status_history WHERE application_id = ?", app));
        assertThrows(BusinessException.class, () -> pipeline.withdraw(101, app), "OFFER is terminal");
    }

    private long count(String sql, Object... args) {
        Long v = jdbc.queryForObject(sql, Long.class, args);
        return v == null ? 0 : v;
    }
}
