package com.swipehire.web.repository;

import com.swipehire.common.enums.ApplicationStatus;
import com.swipehire.web.entity.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** HQL "join fetch" loads the match, job and company in ONE query instead of N+1 queries. */
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    @Query("""
            select a from Application a
              join fetch a.match m join fetch m.job j join fetch j.company
            where m.applicant.userId = :applicantId
            order by a.statusChangedAt desc""")
    List<Application> findForApplicant(Long applicantId);

    @Query("""
            select a from Application a
              join fetch a.match m join fetch m.applicant ap join fetch ap.user
            where m.job.id = :jobId
            order by a.statusChangedAt desc""")
    List<Application> findForJob(Long jobId);

    @Query("select a from Application a where a.match.id = :matchId")
    Optional<Application> findByMatchId(Long matchId);

    /** Candidates for the expiry job: still waiting and untouched since the cutoff. */
    @Query("select a from Application a where a.status in :statuses and a.statusChangedAt < :cutoff")
    List<Application> findStale(Collection<ApplicationStatus> statuses, LocalDateTime cutoff);
}
