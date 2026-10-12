package com.swipehire.web.repository;

import com.swipehire.common.enums.VerificationStatus;
import com.swipehire.web.entity.College;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CollegeRepository extends JpaRepository<College, Long> {
    Optional<College> findByEmailDomainIgnoreCase(String emailDomain);

    List<College> findByStatusOrderByNameAsc(VerificationStatus status);
}
