package com.swipehire.web.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Alternative spelling of a skill, e.g. "k8s" -> Kubernetes. The alias itself is the primary key. */
@Entity
@Table(name = "skill_synonym")
public class SkillSynonym {

    @Id
    @Column(length = 80)
    private String alias;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "skill_id")
    private Skill skill;

    protected SkillSynonym() {
    }

    public SkillSynonym(String alias, Skill skill) {
        this.alias = alias;
        this.skill = skill;
    }

    public String getAlias() { return alias; }
    public Skill getSkill() { return skill; }
}
