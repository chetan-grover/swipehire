package com.swipehire.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread pools are created explicitly with java.util.concurrent so they are visible and
 * explainable. Spring calls shutdown() on these beans when the app stops (destroyMethod).
 */
@Configuration
public class AsyncConfig {

    /** Background work that should not block a web request: PDF generation, emails, deck prefetch. */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService taskPool() {
        return Executors.newFixedThreadPool(4, named("task-"));
    }

    /** Used by the in-process Ranker when the RMI engine is down (degraded mode) and for RMI call timeouts. */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService rankingPool() {
        return Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors()), named("rank-"));
    }

    static ThreadFactory named(String prefix) {
        AtomicInteger n = new AtomicInteger(1);
        return r -> {
            Thread t = new Thread(r, prefix + n.getAndIncrement());
            t.setDaemon(true);
            return t;
        };
    }
}
