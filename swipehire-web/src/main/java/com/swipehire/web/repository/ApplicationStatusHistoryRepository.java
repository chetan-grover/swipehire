package com.swipehire.web.repository;

import com.swipehire.web.entity.ApplicationStatusHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ApplicationStatusHistoryRepository extends JpaRepository<ApplicationStatusHistory, Long> {

    @Query("""
            select h from ApplicationStatusHistory h left join fetch h.changedBy
            where h.application.id = :applicationId
            order by h.changedAt asc, h.id asc""")
    List<ApplicationStatusHistory> findTimeline(Long applicationId);

    /** Admin audit page: newest changes across the platform. */
    @Query("""
            select h from ApplicationStatusHistory h
              left join fetch h.changedBy
              join fetch h.application a join fetch a.match m join fetch m.job j
              join fetch m.applicant ap join fetch ap.user
            order by h.changedAt desc, h.id desc""")
    List<ApplicationStatusHistory> findRecent(Pageable page);
}
