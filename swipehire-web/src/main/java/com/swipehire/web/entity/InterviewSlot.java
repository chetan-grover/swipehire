package com.swipehire.web.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** A time the hirer proposes for an interview; the applicant marks one as chosen. */
@Entity
@Table(name = "interview_slot")
public class InterviewSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "slot_start", nullable = false)
    private LocalDateTime slotStart;

    @Column(name = "duration_min", nullable = false)
    private int durationMin = 30;

    @Column(length = 255)
    private String location;

    @Column(nullable = false)
    private boolean chosen;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected InterviewSlot() {
    }

    public InterviewSlot(Long applicationId, LocalDateTime slotStart, int durationMin, String location) {
        this.applicationId = applicationId;
        this.slotStart = slotStart;
        this.durationMin = durationMin;
        this.location = location;
    }

    public Long getId() { return id; }
    public Long getApplicationId() { return applicationId; }
    public LocalDateTime getSlotStart() { return slotStart; }
    public int getDurationMin() { return durationMin; }
    public String getLocation() { return location; }
    public boolean isChosen() { return chosen; }
    public void setChosen(boolean chosen) { this.chosen = chosen; }
}
