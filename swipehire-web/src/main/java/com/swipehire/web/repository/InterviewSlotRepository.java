package com.swipehire.web.repository;

import com.swipehire.web.entity.InterviewSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InterviewSlotRepository extends JpaRepository<InterviewSlot, Long> {
    List<InterviewSlot> findByApplicationIdOrderBySlotStartAsc(Long applicationId);

    @Modifying
    @Query("delete from InterviewSlot s where s.applicationId = :applicationId")
    void deleteAllForApplication(Long applicationId);
}
