package com.swipehire.web.repository;

import com.swipehire.common.enums.VerificationStatus;
import com.swipehire.web.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository extends JpaRepository<Company, Long> {
    List<Company> findAllByOrderByNameAsc();

    List<Company> findByStatusOrderByCreatedAtAsc(VerificationStatus status);

    Optional<Company> findByNameIgnoreCase(String name);
}
