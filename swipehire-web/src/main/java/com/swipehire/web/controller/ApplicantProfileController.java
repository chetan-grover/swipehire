package com.swipehire.web.controller;

import com.swipehire.common.enums.JobType;
import com.swipehire.common.enums.ResumeItemType;
import com.swipehire.common.enums.VerificationStatus;
import com.swipehire.common.enums.WorkMode;
import com.swipehire.web.entity.ApplicantProfile;
import com.swipehire.web.entity.College;
import com.swipehire.web.form.ProfileForm;
import com.swipehire.web.form.ResumeItemForm;
import com.swipehire.web.repository.CollegeRepository;
import com.swipehire.web.security.AppUserPrincipal;
import com.swipehire.web.service.ProfileService;
import com.swipehire.web.service.SkillCatalog;
import com.swipehire.web.service.VerificationService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;

/** Applicant profile, skills, resume items, PDF import review and college verification. */
@Controller
@RequestMapping("/applicant")
public class ApplicantProfileController {

    /** Session key where ResumeImportServlet leaves the detected skill ids for review. */
    public static final String IMPORT_SUGGESTIONS = "importSuggestions";

    private final ProfileService profiles;
    private final SkillCatalog catalog;
    private final VerificationService verification;
    private final CollegeRepository colleges;

    public ApplicantProfileController(ProfileService profiles, SkillCatalog catalog, VerificationService verification,
                                      CollegeRepository colleges) {
        this.profiles = profiles;
        this.catalog = catalog;
        this.verification = verification;
        this.colleges = colleges;
    }

    // ------------------------------------------------------------- profile

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal AppUserPrincipal me, Model model) {
        ApplicantProfile p = profiles.get(me.getId());
        ProfileForm form = new ProfileForm();
        form.setFullName(p.getUser().getFullName());
        form.setHeadline(p.getHeadline());
        form.setAbout(p.getAbout());
        form.setPhone(p.getPhone());
        form.setCity(p.getCity());
        form.setDegree(p.getDegree());
        form.setGradYear(p.getGradYear());
        form.setPrefWorkMode(p.getPrefWorkMode());
        form.setPrefJobType(p.getPrefJobType());
        form.setMinStipend(p.getMinStipend());
        form.setDiscoverable(p.isDiscoverable());
        form.setPreferredCities(String.join(", ", p.getPreferredCities()));
        model.addAttribute("profile", p);
        model.addAttribute("form", form);
        model.addAttribute("checklist", profiles.checklist(me.getId()));
        model.addAttribute("workModes", WorkMode.values());
        model.addAttribute("jobTypes", JobType.values());
        return "applicant/profile";
    }

    @PostMapping("/profile")
    public String saveProfile(@AuthenticationPrincipal AppUserPrincipal me, @Valid @ModelAttribute ProfileForm form,
                              BindingResult errors, RedirectAttributes flash) {
        if (errors.hasErrors()) {
            flash.addFlashAttribute("error", "Please check the highlighted values: " + errors.getFieldErrors().stream()
                    .map(e -> e.getField()).distinct().toList());
            return "redirect:/applicant/profile";
        }
        profiles.update(me.getId(), form);
        flash.addFlashAttribute("flash", "Profile saved");
        return "redirect:/applicant/profile";
    }

    // ------------------------------------------------------------- skills

    @GetMapping("/skills")
    public String skills(@AuthenticationPrincipal AppUserPrincipal me, Model model) {
        model.addAttribute("profile", profiles.get(me.getId()));
        model.addAttribute("skillsByCategory", catalog.byCategory());
        model.addAttribute("latestUpload", profiles.latestUpload(me.getId()).orElse(null));
        return "applicant/skills";
    }

    @PostMapping("/skills")
    public String setSkill(@AuthenticationPrincipal AppUserPrincipal me, @RequestParam long skillId,
                           @RequestParam int proficiency, RedirectAttributes flash) {
        profiles.setSkill(me.getId(), skillId, proficiency);
        flash.addFlashAttribute("flash", catalog.name(skillId) + " saved");
        return "redirect:/applicant/skills";
    }

    @PostMapping("/skills/{skillId}/remove")
    public String removeSkill(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable long skillId) {
        profiles.removeSkill(me.getId(), skillId);
        return "redirect:/applicant/skills";
    }

    @GetMapping("/skills/import-review")
    public String importReview(HttpSession session, Model model) {
        @SuppressWarnings("unchecked")
        Set<Long> suggested = (Set<Long>) session.getAttribute(IMPORT_SUGGESTIONS);
        if (suggested == null) {
            return "redirect:/applicant/skills";
        }
        model.addAttribute("suggested", suggested.stream().map(catalog::get).filter(s -> s != null).toList());
        return "applicant/import-review";
    }

    @PostMapping("/skills/import-review")
    public String confirmImport(@AuthenticationPrincipal AppUserPrincipal me, HttpSession session,
                                @RequestParam(required = false) List<Long> skillIds, RedirectAttributes flash) {
        session.removeAttribute(IMPORT_SUGGESTIONS);
        int added = skillIds == null ? 0 : profiles.addSkills(me.getId(), skillIds);
        flash.addFlashAttribute("flash", added + " skill(s) added from your resume. Adjust proficiency below.");
        return "redirect:/applicant/skills";
    }

    // ------------------------------------------------------------- resume items

    @GetMapping("/resume")
    public String resume(@AuthenticationPrincipal AppUserPrincipal me, Model model) {
        model.addAttribute("items", profiles.resumeItems(me.getId()));
        model.addAttribute("itemTypes", ResumeItemType.values());
        model.addAttribute("skillsByCategory", catalog.byCategory());
        model.addAttribute("form", new ResumeItemForm());
        return "applicant/resume";
    }

    @PostMapping("/resume")
    public String addItem(@AuthenticationPrincipal AppUserPrincipal me, @ModelAttribute ResumeItemForm form,
                          RedirectAttributes flash) {
        profiles.addResumeItem(me.getId(), form);
        flash.addFlashAttribute("flash", "Added \"" + form.getTitle() + "\"");
        return "redirect:/applicant/resume";
    }

    @PostMapping("/resume/{itemId}/delete")
    public String deleteItem(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable long itemId) {
        profiles.deleteResumeItem(me.getId(), itemId);
        return "redirect:/applicant/resume";
    }

    // ------------------------------------------------------------- college verification

    @GetMapping("/verify")
    public String verify(@AuthenticationPrincipal AppUserPrincipal me, Model model) {
        model.addAttribute("profile", profiles.get(me.getId()));
        List<College> known = colleges.findByStatusOrderByNameAsc(VerificationStatus.VERIFIED);
        model.addAttribute("colleges", known);
        return "applicant/verify";
    }

    @PostMapping("/verify/send")
    public String sendOtp(@AuthenticationPrincipal AppUserPrincipal me, @RequestParam String collegeEmail,
                          RedirectAttributes flash) {
        verification.sendOtp(me.getId(), collegeEmail).ifPresentOrElse(
                code -> flash.addFlashAttribute("flash", "Demo mode (email disabled): your code is " + code
                        + ". It was also printed in the server console."),
                () -> flash.addFlashAttribute("flash", "We sent a 6-digit code to " + collegeEmail));
        flash.addFlashAttribute("otpSent", true);
        flash.addFlashAttribute("collegeEmail", collegeEmail);
        return "redirect:/applicant/verify";
    }

    @PostMapping("/verify/confirm")
    public String confirmOtp(@AuthenticationPrincipal AppUserPrincipal me, @RequestParam String code,
                             RedirectAttributes flash) {
        College c = verification.confirmOtp(me.getId(), code);
        flash.addFlashAttribute("flash", "Verified! You now carry the Verified Student badge for " + c.getName());
        return "redirect:/applicant/verify";
    }

    @PostMapping("/verify/request-college")
    public String requestCollege(@AuthenticationPrincipal AppUserPrincipal me, @RequestParam String name,
                                 @RequestParam String domain, @RequestParam(required = false) String city,
                                 RedirectAttributes flash) {
        verification.requestCollege(me.getId(), name, domain, city);
        flash.addFlashAttribute("flash", "Request sent. You'll be notified when an admin approves " + name);
        return "redirect:/applicant/verify";
    }
}
