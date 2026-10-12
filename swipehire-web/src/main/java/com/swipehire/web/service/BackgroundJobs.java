package com.swipehire.web.service;

import com.swipehire.common.scoring.PreferenceUpdater;
import com.swipehire.web.dao.SwipeDao;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Periodic jobs on a ScheduledExecutorService (2 threads):
 * <ul>
 *   <li>every hour: expire stale matches, send 24h warnings, purge old OTPs</li>
 *   <li>every day: decay learned preferences by 2%</li>
 * </ul>
 * scheduleAtFixedRate never runs the same task twice at once; a task that throws would stop being
 * rescheduled, so every task catches its own exceptions.
 */
@Component
public class BackgroundJobs {

    private static final Logger log = LoggerFactory.getLogger(BackgroundJobs.class);

    private final PipelineService pipeline;
    private final VerificationService verification;
    private final SwipeDao swipeDao;
    private final boolean enabled;
    private ScheduledExecutorService scheduler;

    public BackgroundJobs(PipelineService pipeline, VerificationService verification, SwipeDao swipeDao,
                          @Value("${swipehire.jobs.enabled:true}") boolean enabled) {
        this.pipeline = pipeline;
        this.verification = verification;
        this.swipeDao = swipeDao;
        this.enabled = enabled;
    }

    @PostConstruct
    void start() {
        if (!enabled) {
            return;
        }
        scheduler = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "scheduler");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(safely("hourly maintenance", this::hourly), 1, 60, TimeUnit.MINUTES);
        scheduler.scheduleAtFixedRate(safely("preference decay", this::daily), 10, 24 * 60, TimeUnit.MINUTES);
        log.info("Background jobs scheduled (hourly maintenance, daily preference decay)");
    }

    @PreDestroy
    void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    /** Public so the admin page can trigger it on demand during a demo. */
    public String hourly() {
        int expired = pipeline.expireStale();
        int warned = pipeline.sendExpiryWarnings();
        int purged = verification.purgeExpired();
        String summary = "expired " + expired + " match(es), sent " + warned + " warning(s), purged " + purged + " old OTP(s)";
        log.info("Hourly maintenance: {}", summary);
        return summary;
    }

    void daily() {
        int rows = swipeDao.decayAffinities(PreferenceUpdater.DAILY_DECAY);
        log.info("Preference decay applied to {} rows", rows);
    }

    private static Runnable safely(String name, Runnable task) {
        return () -> {
            try {
                task.run();
            } catch (RuntimeException e) {
                log.error("Background job '{}' failed", name, e);
            }
        };
    }
}
