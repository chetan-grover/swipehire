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

/**
 * Junction table applicant_skill with an extra column (proficiency). Because the link carries
 * data, it is an entity with a composite key (@EmbeddedId) instead of a plain @ManyToMany.
 */
@Entity
@Table(name = "applicant_skill")
public class ApplicantSkill {

    @EmbeddedId
    private Key id;

    @MapsId("applicantId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_id")
    private ApplicantProfile applicant;

    @MapsId("skillId")
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "skill_id")
    private Skill skill;

    @Column(nullable = false)
    private int proficiency;

    protected ApplicantSkill() {
    }

    public ApplicantSkill(ApplicantProfile applicant, Skill skill, int proficiency) {
        this.id = new Key(applicant.getUserId(), skill.getId());
        this.applicant = applicant;
        this.skill = skill;
        this.proficiency = proficiency;
    }

    public Key getId() { return id; }
    public Skill getSkill() { return skill; }
    public int getProficiency() { return proficiency; }
    public void setProficiency(int proficiency) { this.proficiency = proficiency; }

    @Embeddable
    public static class Key implements Serializable {
        @Column(name = "applicant_id")
        private Long applicantId;
        @Column(name = "skill_id")
        private Long skillId;

        protected Key() {
        }

        public Key(Long applicantId, Long skillId) {
            this.applicantId = applicantId;
            this.skillId = skillId;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key k && Objects.equals(applicantId, k.applicantId) && Objects.equals(skillId, k.skillId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(applicantId, skillId);
        }
    }
}
