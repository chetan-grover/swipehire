package com.swipehire.common.rmi;

import com.swipehire.common.dto.ApplicantData;
import com.swipehire.common.dto.JobData;
import com.swipehire.common.dto.ScoreBreakdown;
import com.swipehire.common.enums.SwipeDirection;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

/**
 * Remote interface of the match engine JVM. Both the engine (server) and the web app (client)
 * depend on this one interface, which is why it lives in the common module. Every parameter and
 * return type is Serializable, because RMI copies them between the two JVMs.
 */
public interface MatchEngine extends Remote {

    /** Name the engine is bound under in the RMI registry. */
    String BINDING_NAME = "SwipeHireMatchEngine";
    int REGISTRY_PORT = 1099;

    /** Health check used by the admin dashboard. */
    String ping() throws RemoteException;

    /** Ranked, explainable job cards for an applicant's deck (hard filters + score + exploration). */
    List<ScoreBreakdown> rankJobsForApplicant(ApplicantData applicant, List<JobData> candidateJobs, int limit)
            throws RemoteException;

    /** Ranked applicants for a hirer's queue or Discover tab. */
    List<ScoreBreakdown> rankApplicantsForJob(JobData job, List<ApplicantData> candidates) throws RemoteException;

    /** New affinity values after a swipe (or after undoing one). Returns only changed skills. */
    Map<Long, Double> updatePreferences(Map<Long, Double> currentAffinity, List<Long> jobSkillIds,
                                        SwipeDirection direction, boolean undo) throws RemoteException;
}
