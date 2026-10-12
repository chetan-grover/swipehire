package com.swipehire.web.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** A recruiter: 1:1 with users (shared primary key), N:1 with company. */
@Entity
@Table(name = "hirer")
public class Hirer {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(length = 100)
    private String designation;

    protected Hirer() {
    }

    public Hirer(User user, Company company, String designation) {
        this.user = user;
        this.company = company;
        this.designation = designation;
    }

    public Long getUserId() { return userId; }
    public User getUser() { return user; }
    public Company getCompany() { return company; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
}
