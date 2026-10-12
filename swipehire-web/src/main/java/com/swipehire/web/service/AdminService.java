package com.swipehire.web.service;

import com.swipehire.common.enums.JobStatus;
import com.swipehire.common.enums.NotificationType;
import com.swipehire.common.enums.Role;
import com.swipehire.common.enums.VerificationStatus;
import com.swipehire.web.config.AppTime;
import com.swipehire.web.entity.College;
import com.swipehire.web.entity.Company;
import com.swipehire.web.entity.Hirer;
import com.swipehire.web.entity.Job;
import com.swipehire.web.entity.Skill;
import com.swipehire.web.entity.SkillSynonym;
import com.swipehire.web.entity.User;
import com.swipehire.web.repository.CollegeRepository;
import com.swipehire.web.repository.CompanyRepository;
import com.swipehire.web.repository.HirerRepository;
import com.swipehire.web.repository.JobRepository;
import com.swipehire.web.repository.SkillRepository;
import com.swipehire.web.repository.SkillSynonymRepository;
import com.swipehire.web.repository.UserRepository;
import com.swipehire.web.support.BusinessException;
import com.swipehire.web.support.NotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Moderation and dictionary management for ADMIN users. */
@Service
public class AdminService {

    private final EntityManager em;
    private final CompanyRepository companies;
    private final CollegeRepository colleges;
    private final UserRepository users;
    private final HirerRepository hirers;
    private final JobRepository jobs;
    private final SkillRepository skills;
    private final SkillSynonymRepository synonyms;
    private final SkillCatalog catalog;
    private final NotificationService notifications;

    public AdminService(EntityManager em, CompanyRepository companies, CollegeRepository colleges, UserRepository users,
                        HirerRepository hirers, JobRepository jobs, SkillRepository skills,
                        SkillSynonymRepository synonyms, SkillCatalog catalog, NotificationService notifications) {
        this.em = em;
        this.companies = companies;
        this.colleges = colleges;
        this.users = users;
        this.hirers = hirers;
        this.jobs = jobs;
        this.skills = skills;
        this.synonyms = synonyms;
        this.catalog = catalog;
        this.notifications = notifications;
    }

    // ------------------------------------------------------------- companies & colleges

    @Transactional
    public void setCompanyStatus(long companyId, VerificationStatus status, long adminId) {
        Company c = companies.findById(companyId).orElseThrow(() -> new NotFoundException("Company not found"));
        c.setStatus(status);
        c.setVerifiedBy(adminId);
        c.setVerifiedAt(AppTime.now());
        String title = status == VerificationStatus.VERIFIED
                ? c.getName() + " is now a Verified Company" : c.getName() + " could not be verified";
        for (Hirer h : hirers.findAll()) {
            if (h.getCompany().getId().equals(companyId)) {
                notifications.notify(h.getUserId(), NotificationType.VERIFICATION, title, null, "/hirer/dashboard");
            }
        }
    }

    @Transactional
    public void setCollegeStatus(long collegeId, VerificationStatus status) {
        College c = colleges.findById(collegeId).orElseThrow(() -> new NotFoundException("College not found"));
        c.setStatus(status);
        if (c.getRequestedBy() != null) {
            notifications.notify(c.getRequestedBy(), NotificationType.VERIFICATION,
                    status == VerificationStatus.VERIFIED ? c.getName() + " was added: verify your college email now"
                            : "Your college request for " + c.getName() + " was rejected",
                    null, "/applicant/verify");
        }
    }

    @Transactional
    public void addCollege(String name, String domain, String city) {
        String d = domain == null ? "" : domain.trim().toLowerCase(Locale.ROOT);
        if (name == null || name.isBlank() || !d.matches("^[a-z0-9.-]+\\.[a-z]{2,}$")) {
            throw new BusinessException("Enter a name and a valid email domain");
        }
        if (colleges.findByEmailDomainIgnoreCase(d).isPresent()) {
            throw new BusinessException("Domain already exists");
        }
        colleges.save(new College(name.trim(), d, AccountService.trimToNull(city), VerificationStatus.VERIFIED, null));
    }

    // ------------------------------------------------------------- users & jobs

    @Transactional
    public void toggleUser(long userId, long adminId) {
        if (userId == adminId) {
            throw new BusinessException("You can't disable your own account");
        }
        User u = users.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
        u.setEnabled(!u.isEnabled());
    }

    @Transactional
    public void closeJob(long jobId) {
        jobs.findById(jobId).orElseThrow(() -> new NotFoundException("Job not found")).setStatus(JobStatus.CLOSED);
    }

    /**
     * JPA Criteria API: the WHERE clause is assembled from whichever filters the admin filled in,
     * with type-safe predicates instead of concatenating SQL strings.
     */
    @Transactional(readOnly = true)
    public List<User> searchUsers(String text, Role role, Boolean enabled) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<User> q = cb.createQuery(User.class);
        Root<User> u = q.from(User.class);
        List<Predicate> where = new ArrayList<>();
        if (text != null && !text.isBlank()) {
            String like = "%" + text.trim().toLowerCase(Locale.ROOT) + "%";
            where.add(cb.or(cb.like(cb.lower(u.get("email")), like), cb.like(cb.lower(u.get("fullName")), like)));
        }
        if (role != null) {
            where.add(cb.equal(u.get("role"), role));
        }
        if (enabled != null) {
            where.add(cb.equal(u.get("enabled"), enabled));
        }
        q.select(u).where(where.toArray(Predicate[]::new)).orderBy(cb.desc(u.get("createdAt")));
        return em.createQuery(q).setMaxResults(200).getResultList();
    }

    @Transactional(readOnly = true)
    public List<Job> searchJobs(String text, JobStatus status) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Job> q = cb.createQuery(Job.class);
        Root<Job> j = q.from(Job.class);
        Join<Job, Company> c = j.join("company");
        List<Predicate> where = new ArrayList<>();
        if (text != null && !text.isBlank()) {
            String like = "%" + text.trim().toLowerCase(Locale.ROOT) + "%";
            where.add(cb.or(cb.like(cb.lower(j.get("title")), like), cb.like(cb.lower(c.get("name")), like)));
        }
        if (status != null) {
            where.add(cb.equal(j.get("status"), status));
        }
        q.select(j).where(where.toArray(Predicate[]::new)).orderBy(cb.desc(j.get("createdAt")));
        return em.createQuery(q).setMaxResults(200).getResultList();
    }

    // ------------------------------------------------------------- skill dictionary

    @Transactional
    public void addSkill(String name, String category) {
        if (name == null || name.isBlank() || category == null || category.isBlank()) {
            throw new BusinessException("Skill name and category are required");
        }
        if (skills.findByNameIgnoreCase(name.trim()).isPresent()) {
            throw new BusinessException("Skill already exists");
        }
        skills.save(new Skill(name.trim(), category.trim()));
        catalog.reload();
    }

    @Transactional
    public void addSynonym(String alias, long skillId) {
        String a = alias == null ? "" : alias.trim().toLowerCase(Locale.ROOT);
        if (a.length() < 2) {
            throw new BusinessException("Alias must be at least 2 characters");
        }
        if (synonyms.existsById(a)) {
            throw new BusinessException("Alias already exists");
        }
        synonyms.save(new SkillSynonym(a, skills.findById(skillId).orElseThrow(() -> new BusinessException("Unknown skill"))));
    }

    @Transactional
    public void deleteSynonym(String alias) {
        synonyms.deleteById(alias);
    }
}
