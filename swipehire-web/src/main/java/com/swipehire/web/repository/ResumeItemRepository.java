package com.swipehire.web.repository;

import com.swipehire.web.entity.ResumeItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ResumeItemRepository extends JpaRepository<ResumeItem, Long> {
    List<ResumeItem> findByApplicantIdOrderByDisplayOrderAsc(Long applicantId);

    @Query("select coalesce(max(r.displayOrder), 0) from ResumeItem r where r.applicantId = :applicantId")
    int maxDisplayOrder(Long applicantId);
}
