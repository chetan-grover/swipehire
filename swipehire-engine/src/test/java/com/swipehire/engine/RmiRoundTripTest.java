package com.swipehire.engine;

import com.swipehire.common.dto.ApplicantData;
import com.swipehire.common.dto.JobData;
import com.swipehire.common.dto.ScoreBreakdown;
import com.swipehire.common.enums.SwipeDirection;
import com.swipehire.common.rmi.MatchEngine;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

/** Starts a real RMI registry, binds the engine and calls it through the stub, like the web app does. */
class RmiRoundTripTest {

    static final int PORT = 1199;   // not 1099, so the test doesn't clash with a running engine
    static ExecutorService pool;
    static MatchEngineImpl impl;
    static MatchEngine stub;

    @BeforeAll
    static void start() throws Exception {
        System.setProperty("java.rmi.server.hostname", "localhost");
        pool = Executors.newFixedThreadPool(2);
        impl = new MatchEngineImpl(pool, 2);
        Registry registry = LocateRegistry.createRegistry(PORT);
        registry.rebind(MatchEngine.BINDING_NAME, impl);
        stub = (MatchEngine) LocateRegistry.getRegistry("localhost", PORT).lookup(MatchEngine.BINDING_NAME);
    }

    @AfterAll
    static void stop() throws Exception {
        UnicastRemoteObject.unexportObject(impl, true);
        pool.shutdownNow();
    }

    @Test
    void rankingThroughTheStubReturnsSerializedResults() throws Exception {
        ApplicantData a = new ApplicantData();
        a.setId(1);
        a.setSkillProficiency(Map.of(1L, 4));
        a.setText("java developer");
        JobData j = new JobData();
        j.setId(9);
        j.setText("java backend role");
        j.setSkillWeights(Map.of(1L, 5));
        j.setRequiredSkillIds(Set.of(1L));

        List<ScoreBreakdown> result = stub.rankJobsForApplicant(a, List.of(j), 10);
        assertEquals(1, result.size());
        assertEquals(9L, result.get(0).getJobId());
        assertEquals(1.0, result.get(0).getSkillCoverage(), 1e-9);
        assertTrue(stub.ping().startsWith("engine up since"));
    }

    @Test
    void preferenceUpdateRunsRemotely() throws Exception {
        Map<Long, Double> updated = stub.updatePreferences(Map.of(), List.of(3L), SwipeDirection.RIGHT, false);
        assertEquals(0.10, updated.get(3L), 1e-9);
    }
}
