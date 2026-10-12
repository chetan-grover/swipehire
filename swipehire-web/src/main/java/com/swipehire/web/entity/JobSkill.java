package com.swipehire.web.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

/** job_skill junction with extra columns (weight, is_required), hence an entity with a composite key. */
@Entity
@Table(name = "job_skill")
public class JobSkill {

    @EmbeddedId
    private Key id;

    @MapsId("jobId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id")
    private Job job;

    @MapsId("skillId")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "skill_id")
    private Skill skill;

    @Column(nullable = false)
    private int weight;

    @Column(name = "is_required", nullable = false)
    private boolean required;

    protected JobSkill() {
    }

    public JobSkill(Job job, Skill skill, int weight, boolean required) {
        this.id = new Key(job.getId(), skill.getId());
        this.job = job;
        this.skill = skill;
        this.weight = weight;
        this.required = required;
    }

    public Skill getSkill() { return skill; }
    public int getWeight() { return weight; }
    public void setWeight(int weight) { this.weight = weight; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }

    @Embeddable
    public static class Key implements Serializable {
        @Column(name = "job_id")
        private Long jobId;
        @Column(name = "skill_id")
        private Long skillId;

        protected Key() {
        }

        public Key(Long jobId, Long skillId) {
            this.jobId = jobId;
            this.skillId = skillId;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key k && Objects.equals(jobId, k.jobId) && Objects.equals(skillId, k.skillId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(jobId, skillId);
        }
    }
}
