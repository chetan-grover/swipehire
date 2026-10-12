package com.swipehire.web.service;

import com.swipehire.common.enums.JobStatus;
import com.swipehire.common.enums.WorkMode;
import com.swipehire.web.config.AppTime;
import com.swipehire.web.entity.Company;
import com.swipehire.web.entity.Hirer;
import com.swipehire.web.entity.Job;
import com.swipehire.web.entity.JobSkill;
import com.swipehire.web.entity.Skill;
import com.swipehire.web.form.JobForm;
import com.swipehire.web.repository.CollegeRepository;
import com.swipehire.web.repository.HirerRepository;
import com.swipehire.web.repository.JobRepository;
import com.swipehire.web.repository.SkillRepository;
import com.swipehire.web.support.BusinessException;
import com.swipehire.web.support.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Job posting and job state changes for hirers. */
@Service
public class JobService {

    private final JobRepository jobs;
    private final HirerRepository hirers;
    private final SkillRepository skills;
    private final CollegeRepository colleges;

    public JobService(JobRepository jobs, HirerRepository hirers, SkillRepository skills, CollegeRepository colleges) {
        this.jobs = jobs;
        this.hirers = hirers;
        this.skills = skills;
        this.colleges = colleges;
    }

    @Transactional(readOnly = true)
    public Hirer hirer(long hirerUserId) {
        return hirers.findById(hirerUserId).orElseThrow(() -> new NotFoundException("Hirer profile not found"));
    }

    @Transactional(readOnly = true)
    public List<Job> jobsOfCompany(long companyId) {
        return jobs.findByCompanyIdOrderByCreatedAtDesc(companyId);
    }

    /** Any recruiter of the same company may manage the job. Others get "not found". */
    @Transactional(readOnly = true)
    public Job requireOwned(long jobId, long hirerUserId) {
        Hirer h = hirer(hirerUserId);
        return jobs.findById(jobId)
                .filter(j -> j.getCompany().getId().equals(h.getCompany().getId()))
                .orElseThrow(() -> new NotFoundException("Job not found"));
    }

    @Transactional
    public void updateCompany(long hirerUserId, String website, String city, String description) {
        Company c = hirer(hirerUserId).getCompany();
        c.setWebsite(AccountService.trimToNull(website));
        c.setCity(AccountService.trimToNull(city));
        c.setDescription(AccountService.trimToNull(description));
    }

    @Transactional
    public Job create(long hirerUserId, JobForm form) {
        Hirer h = hirer(hirerUserId);
        Job job = jobs.save(new Job(h.getCompany(), hirerUserId));   // save first: JobSkill keys need the job id
        apply(job, form);
        setStatus(job, form.isPublish() ? JobStatus.ACTIVE : JobStatus.DRAFT);
        return job;
    }

    @Transactional
    public void update(long jobId, long hirerUserId, JobForm form) {
        apply(requireOwned(jobId, hirerUserId), form);
    }

    @Transactional
    public void changeStatus(long jobId, long hirerUserId, JobStatus status) {
        setStatus(requireOwned(jobId, hirerUserId), status);
    }

    private void setStatus(Job job, JobStatus status) {
        if (status == JobStatus.ACTIVE && job.getStatus() != JobStatus.ACTIVE) {
            if (job.getDeadline().isBefore(AppTime.today())) {
                throw new BusinessException("Move the deadline into the future before publishing");
            }
            Company c = job.getCompany();
            if (!c.isVerified() && jobs.countByCompanyIdAndStatus(c.getId(), JobStatus.ACTIVE)
                    >= Company.UNVERIFIED_ACTIVE_JOB_LIMIT) {
                throw new BusinessException("Unverified companies can have at most " + Company.UNVERIFIED_ACTIVE_JOB_LIMIT
                        + " active jobs. An admin will verify " + c.getName() + " soon.");
            }
        }
        job.setStatus(status);
    }

    private void apply(Job job, JobForm f) {
        if (f.getTitle() == null || f.getTitle().isBlank() || f.getDescription() == null || f.getDescription().isBlank()) {
            throw new BusinessException("Title and description are required");
        }
        if (f.getWorkMode() == WorkMode.ANY) {
            throw new BusinessException("Choose Remote, Hybrid or Onsite");
        }
        if (f.getWorkMode() != WorkMode.REMOTE && (f.getCity() == null || f.getCity().isBlank())) {
            throw new BusinessException("Enter a city for hybrid/onsite jobs");
        }
        if (f.getStipendMin() < 0 || f.getStipendMax() < f.getStipendMin()) {
            throw new BusinessException("Stipend range is invalid");
        }
        if (f.getDeadline() == null) {
            throw new BusinessException("Set an application deadline");
        }
        if (f.getMinGradYear() != null && f.getMaxGradYear() != null && f.getMaxGradYear() < f.getMinGradYear()) {
            throw new BusinessException("Graduation year range is invalid");
        }
        if (f.getSkillIds().isEmpty()) {
            throw new BusinessException("Pick at least one skill: matching depends on it");
        }
        job.setTitle(f.getTitle().trim());
        job.setDescription(f.getDescription().trim());
        job.setJobType(f.getJobType());
        job.setWorkMode(f.getWorkMode());
        job.setCity(f.getWorkMode() == WorkMode.REMOTE ? AccountService.trimToNull(f.getCity()) : f.getCity().trim());
        job.setStipendMin(f.getStipendMin());
        job.setStipendMax(f.getStipendMax());
        job.setOpenings(Math.max(1, f.getOpenings()));
        job.setDeadline(f.getDeadline());
        job.setMinGradYear(f.getMinGradYear());
        job.setMaxGradYear(f.getMaxGradYear());
        job.setVerifiedOnly(f.isVerifiedOnly());
        syncSkills(job, f);
        job.getAllowedColleges().clear();
        job.getAllowedColleges().addAll(colleges.findAllById(f.getAllowedCollegeIds()));
    }

    /**
     * Updates skill rows in place instead of "delete all, insert all": Hibernate flushes inserts
     * before deletes, so re-adding a row with the same (job_id, skill_id) key would violate the PK.
     */
    private void syncSkills(Job job, JobForm f) {
        Set<Long> wanted = new HashSet<>(f.getSkillIds());
        Set<Long> required = new HashSet<>(f.getRequiredSkillIds());
        job.getSkills().removeIf(js -> !wanted.contains(js.getSkill().getId()));
        for (Long skillId : wanted) {
            int weight = Math.max(1, Math.min(5, f.getWeights().getOrDefault(skillId, 3)));
            boolean req = required.contains(skillId);
            JobSkill existing = job.getSkills().stream()
                    .filter(js -> js.getSkill().getId().equals(skillId)).findFirst().orElse(null);
            if (existing != null) {
                existing.setWeight(weight);
                existing.setRequired(req);
            } else {
                Skill skill = skills.findById(skillId).orElseThrow(() -> new BusinessException("Unknown skill"));
                job.getSkills().add(new JobSkill(job, skill, weight, req));
            }
        }
    }
}
