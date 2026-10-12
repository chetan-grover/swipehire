package com.swipehire.engine;

import com.swipehire.common.rmi.MatchEngine;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/** Entry point of the engine JVM: starts an RMI registry and binds the engine into it. */
public class EngineServer {

    public static void main(String[] args) throws Exception {
        // On machines with several network adapters RMI may advertise an unreachable IP;
        // pinning the hostname keeps the local demo reliable.
        System.setProperty("java.rmi.server.hostname", "localhost");

        // One worker per CPU core: scoring is CPU-bound, so more threads would only add switching.
        int threads = Runtime.getRuntime().availableProcessors();
        ExecutorService rankingPool = Executors.newFixedThreadPool(threads, namedThreads("rank-worker-"));

        Registry registry = LocateRegistry.createRegistry(MatchEngine.REGISTRY_PORT);
        registry.rebind(MatchEngine.BINDING_NAME, new MatchEngineImpl(rankingPool, threads));
        System.out.println("[engine] bound '" + MatchEngine.BINDING_NAME + "' on RMI port "
                + MatchEngine.REGISTRY_PORT + " with " + threads + " ranking threads");

        // Block main so the process (and the exported object) stays alive until Ctrl+C.
        CountDownLatch shutdown = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[engine] shutting down");
            rankingPool.shutdown();
            try {
                rankingPool.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            shutdown.countDown();
        }));
        shutdown.await();
    }

    /** Named threads make the engine log readable ("rank-worker-3 took 4 ms"). */
    static ThreadFactory namedThreads(String prefix) {
        AtomicInteger n = new AtomicInteger(1);
        return r -> {
            Thread t = new Thread(r, prefix + n.getAndIncrement());
            t.setDaemon(true);
            return t;
        };
    }
}
