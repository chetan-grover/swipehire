package com.swipehire.web.repository;

import com.swipehire.common.enums.JobStatus;
import com.swipehire.web.entity.Job;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {
    List<Job> findByCompanyIdOrderByCreatedAtDesc(Long companyId);

    long countByCompanyIdAndStatus(Long companyId, JobStatus status);
}
