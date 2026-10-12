package com.swipehire.web.repository;

import com.swipehire.web.entity.JobMatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobMatchRepository extends JpaRepository<JobMatch, Long> {
}
