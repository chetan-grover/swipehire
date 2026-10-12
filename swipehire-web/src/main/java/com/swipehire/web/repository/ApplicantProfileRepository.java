package com.swipehire.web.repository;

import com.swipehire.web.entity.ApplicantProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicantProfileRepository extends JpaRepository<ApplicantProfile, Long> {
    boolean existsByCollegeEmailIgnoreCase(String collegeEmail);
}
