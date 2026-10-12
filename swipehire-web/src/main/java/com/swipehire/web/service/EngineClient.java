package com.swipehire.web.service;

import com.swipehire.common.dto.ApplicantData;
import com.swipehire.common.dto.JobData;
import com.swipehire.common.dto.ScoreBreakdown;
import com.swipehire.common.enums.SwipeDirection;
import com.swipehire.common.rmi.MatchEngine;
import com.swipehire.common.scoring.PreferenceUpdater;
import com.swipehire.common.scoring.Ranker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * RMI client for the match engine, with a fallback.
 * <p>
 * Every call goes to the engine JVM through the RMI stub, with a 2-second timeout. If the engine
 * is down, slow or not started, the SAME Ranker code runs inside this JVM instead ("degraded
 * mode"), so users never see an error. The admin dashboard shows which mode was used.
 */
@Component
public class EngineClient {

    private static final Logger log = LoggerFactory.getLogger(EngineClient.class);

    @FunctionalInterface
    private interface RemoteCall<T> {
        T call(MatchEngine engine) throws Exception;
    }

    private final boolean enabled;
    private final String host;
    private final int port;
    private final long timeoutMs;
    private final Ranker localRanker;
    /** Separate unbounded pool just for waiting on RMI calls, so it can never starve the task pool. */
    private final ExecutorService rmiCallPool = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "rmi-call");
        t.setDaemon(true);
        return t;
    });

    private volatile MatchEngine stub;
    private final AtomicLong remoteCalls = new AtomicLong();
    private final AtomicLong degradedCalls = new AtomicLong();
    private volatile long lastLatencyMs;
    private volatile String lastMode = "none yet";

    public EngineClient(ExecutorService rankingPool,
                        @Value("${swipehire.engine.enabled:true}") boolean enabled,
                        @Value("${swipehire.engine.host:localhost}") String host,
                        @Value("${swipehire.engine.port:1099}") int port,
                        @Value("${swipehire.engine.timeout-ms:2000}") long timeoutMs) {
        this.enabled = enabled;
        this.host = host;
        this.port = port;
        this.timeoutMs = timeoutMs;
        this.localRanker = new Ranker(rankingPool, Runtime.getRuntime().availableProcessors(), new Random());
    }

    public List<ScoreBreakdown> rankJobs(ApplicantData applicant, List<JobData> jobs, int limit) {
        return call(e -> e.rankJobsForApplicant(applicant, jobs, limit), () -> localRanker.rankJobs(applicant, jobs, limit));
    }

    public List<ScoreBreakdown> rankApplicants(JobData job, List<ApplicantData> applicants) {
        return call(e -> e.rankApplicantsForJob(job, applicants), () -> localRanker.rankApplicants(job, applicants));
    }

    public Map<Long, Double> updatePreferences(Map<Long, Double> current, List<Long> skillIds,
                                               SwipeDirection direction, boolean undo) {
        return call(e -> e.updatePreferences(current, skillIds, direction, undo),
                () -> PreferenceUpdater.apply(current, skillIds, direction, undo));
    }

    private <T> T call(RemoteCall<T> remote, Supplier<T> local) {
        long start = System.nanoTime();
        if (enabled) {
            try {
                MatchEngine engine = stub();
                Future<T> future = rmiCallPool.submit(() -> remote.call(engine));
                T result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
                remoteCalls.incrementAndGet();
                record("RMI engine", start);
                return result;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                // Forget the stub: the next call will look the engine up again (it may have restarted).
                stub = null;
                log.warn("Match engine unreachable ({}); running in DEGRADED mode", rootMessage(e));
            }
        }
        T result = local.get();
        degradedCalls.incrementAndGet();
        record(enabled ? "DEGRADED (local fallback)" : "LOCAL (engine disabled)", start);
        return result;
    }

    private MatchEngine stub() throws Exception {
        MatchEngine s = stub;
        if (s == null) {
            Registry registry = LocateRegistry.getRegistry(host, port);
            s = (MatchEngine) registry.lookup(MatchEngine.BINDING_NAME);
            stub = s;
        }
        return s;
    }

    /** Health check for the admin page. */
    public String ping() {
        if (!enabled) {
            return "disabled in configuration";
        }
        try {
            MatchEngine engine = stub();
            return rmiCallPool.submit(engine::ping).get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            stub = null;
            return "UNREACHABLE: " + rootMessage(e);
        }
    }

    private void record(String mode, long startNanos) {
        lastMode = mode;
        lastLatencyMs = (System.nanoTime() - startNanos) / 1_000_000;
    }

    private static String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t.getClass().getSimpleName() + (t.getMessage() == null ? "" : ": " + t.getMessage());
    }

    public long getRemoteCalls() { return remoteCalls.get(); }
    public long getDegradedCalls() { return degradedCalls.get(); }
    public long getLastLatencyMs() { return lastLatencyMs; }
    public String getLastMode() { return lastMode; }
    public String getEndpoint() { return "rmi://" + host + ":" + port + "/" + MatchEngine.BINDING_NAME; }
}
