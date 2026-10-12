package com.swipehire.web.service;

import com.swipehire.common.tailoring.SkillDetector;
import com.swipehire.web.entity.ApplicantProfile;
import com.swipehire.web.entity.ApplicantSkill;
import com.swipehire.web.entity.ResumeItem;
import com.swipehire.web.entity.ResumeUpload;
import com.swipehire.web.entity.Skill;
import com.swipehire.web.form.ProfileForm;
import com.swipehire.web.form.ResumeItemForm;
import com.swipehire.web.repository.ApplicantProfileRepository;
import com.swipehire.web.repository.ResumeItemRepository;
import com.swipehire.web.repository.ResumeUploadRepository;
import com.swipehire.web.repository.SkillRepository;
import com.swipehire.web.support.BusinessException;
import com.swipehire.web.support.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Applicant profile, skills and structured resume items (Hibernate entity CRUD). */
@Service
public class ProfileService {

    private final ApplicantProfileRepository profiles;
    private final SkillRepository skills;
    private final ResumeItemRepository items;
    private final ResumeUploadRepository uploads;
    private final SkillCatalog catalog;

    public ProfileService(ApplicantProfileRepository profiles, SkillRepository skills, ResumeItemRepository items,
                          ResumeUploadRepository uploads, SkillCatalog catalog) {
        this.profiles = profiles;
        this.skills = skills;
        this.items = items;
        this.uploads = uploads;
        this.catalog = catalog;
    }

    @Transactional(readOnly = true)
    public ApplicantProfile get(long userId) {
        return profiles.findById(userId).orElseThrow(() -> new NotFoundException("Profile not found"));
    }

    @Transactional
    public void update(long userId, ProfileForm f) {
        ApplicantProfile p = get(userId);
        if (f.getFullName() != null && !f.getFullName().isBlank()) {
            p.getUser().setFullName(f.getFullName().trim());
        }
        p.setHeadline(AccountService.trimToNull(f.getHeadline()));
        p.setAbout(AccountService.trimToNull(f.getAbout()));
        p.setPhone(AccountService.trimToNull(f.getPhone()));
        p.setCity(AccountService.trimToNull(f.getCity()));
        p.setDegree(AccountService.trimToNull(f.getDegree()));
        p.setGradYear(f.getGradYear());
        p.setPrefWorkMode(f.getPrefWorkMode());
        p.setPrefJobType(f.getPrefJobType());
        p.setMinStipend(Math.max(0, f.getMinStipend()));
        p.setDiscoverable(f.isDiscoverable());
        Set<String> cities = f.getPreferredCities() == null ? Set.of()
                : Arrays.stream(f.getPreferredCities().split(","))
                .map(String::trim).filter(c -> !c.isEmpty()).limit(10)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        p.getPreferredCities().clear();
        p.getPreferredCities().addAll(cities);
    }

    /** Adds the skill or updates its proficiency if the applicant already has it. */
    @Transactional
    public void setSkill(long userId, long skillId, int proficiency) {
        if (proficiency < 1 || proficiency > 5) {
            throw new BusinessException("Proficiency must be between 1 and 5");
        }
        ApplicantProfile p = get(userId);
        Optional<ApplicantSkill> existing = p.getSkills().stream()
                .filter(s -> s.getSkill().getId().equals(skillId)).findFirst();
        if (existing.isPresent()) {
            existing.get().setProficiency(proficiency);
        } else {
            Skill skill = skills.findById(skillId).orElseThrow(() -> new BusinessException("Unknown skill"));
            p.getSkills().add(new ApplicantSkill(p, skill, proficiency));
        }
    }

    @Transactional
    public void removeSkill(long userId, long skillId) {
        get(userId).getSkills().removeIf(s -> s.getSkill().getId().equals(skillId));
    }

    /** Adds the confirmed skills from a PDF import at a default proficiency of 3 (editable later). */
    @Transactional
    public int addSkills(long userId, Collection<Long> skillIds) {
        ApplicantProfile p = get(userId);
        Set<Long> have = p.getSkills().stream().map(s -> s.getSkill().getId()).collect(Collectors.toSet());
        int added = 0;
        for (Long id : skillIds) {
            if (!have.contains(id)) {
                skills.findById(id).ifPresent(skill -> p.getSkills().add(new ApplicantSkill(p, skill, 3)));
                added++;
            }
        }
        return added;
    }

    @Transactional(readOnly = true)
    public List<ResumeItem> resumeItems(long userId) {
        return items.findByApplicantIdOrderByDisplayOrderAsc(userId);
    }

    @Transactional
    public void addResumeItem(long userId, ResumeItemForm f) {
        if (f.getTitle() == null || f.getTitle().isBlank()) {
            throw new BusinessException("Give the item a title");
        }
        if (f.getStartDate() != null && f.getEndDate() != null && f.getEndDate().isBefore(f.getStartDate())) {
            throw new BusinessException("End date can't be before the start date");
        }
        ResumeItem item = new ResumeItem(userId, f.getItemType(), f.getTitle().trim());
        item.setOrganization(AccountService.trimToNull(f.getOrganization()));
        item.setStartDate(f.getStartDate());
        item.setEndDate(f.getEndDate());
        item.setDescription(AccountService.trimToNull(f.getDescription()));
        item.setDisplayOrder(items.maxDisplayOrder(userId) + 1);
        item.getSkills().addAll(skills.findAllById(f.getSkillIds()));
        items.save(item);
    }

    @Transactional
    public void deleteResumeItem(long userId, long itemId) {
        ResumeItem item = items.findById(itemId).filter(i -> i.getApplicantId().equals(userId))
                .orElseThrow(() -> new NotFoundException("Resume item not found"));
        items.delete(item);
    }

    @Transactional
    public void setPhoto(long userId, String path) {
        get(userId).setPhotoPath(path);
    }

    /**
     * Saves the PDFBox-extracted text and returns skill ids found in it that the applicant does
     * not have yet. These are only suggestions; nothing is added until the applicant confirms.
     */
    @Transactional
    public Set<Long> recordUploadAndSuggest(long userId, String fileName, String path, String text) {
        uploads.save(new ResumeUpload(userId, fileName, path, text));
        Set<Long> found = new SkillDetector(catalog.detectionTerms()).detect(text);
        get(userId).getSkills().forEach(s -> found.remove(s.getSkill().getId()));
        return found;
    }

    @Transactional(readOnly = true)
    public Optional<ResumeUpload> latestUpload(long userId) {
        return uploads.findTopByApplicantIdOrderByUploadedAtDesc(userId);
    }

    /** Checklist shown on the deck/profile so new users know what improves their matches. */
    @Transactional(readOnly = true)
    public ProfileChecklist checklist(long userId) {
        ApplicantProfile p = get(userId);
        return new ProfileChecklist(p.getHeadline() != null && p.getGradYear() != null,
                !p.getSkills().isEmpty(), !items.findByApplicantIdOrderByDisplayOrderAsc(userId).isEmpty(),
                p.isCollegeVerified());
    }

    public record ProfileChecklist(boolean basics, boolean skills, boolean resume, boolean verified) {
        public boolean isBasics() { return basics; }
        public boolean isSkills() { return skills; }
        public boolean isResume() { return resume; }
        public boolean isVerified() { return verified; }
        public boolean isComplete() { return basics && skills && resume; }
    }
}
