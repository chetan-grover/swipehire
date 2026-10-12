package com.swipehire.web.controller;

import com.swipehire.common.dto.ScoreBreakdown;
import com.swipehire.common.enums.ApplicationStatus;
import com.swipehire.common.enums.HirerSwipeSource;
import com.swipehire.common.enums.JobStatus;
import com.swipehire.common.enums.JobType;
import com.swipehire.common.enums.SwipeDirection;
import com.swipehire.common.enums.VerificationStatus;
import com.swipehire.common.enums.WorkMode;
import com.swipehire.web.dao.AnalyticsDao;
import com.swipehire.web.entity.Application;
import com.swipehire.web.entity.Hirer;
import com.swipehire.web.entity.Job;
import com.swipehire.web.form.JobForm;
import com.swipehire.web.repository.CollegeRepository;
import com.swipehire.web.security.AppUserPrincipal;
import com.swipehire.web.service.ChatService;
import com.swipehire.web.service.JobService;
import com.swipehire.web.service.PipelineService;
import com.swipehire.web.service.ProfileService;
import com.swipehire.web.service.ReviewService;
import com.swipehire.web.service.SkillCatalog;
import com.swipehire.web.service.SwipeService;
import com.swipehire.web.view.CandidateCard;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Everything a recruiter does: company profile, jobs, reviewing candidates, the pipeline, analytics. */
@Controller
@RequestMapping("/hirer")
public class HirerController {

    private final JobService jobs;
    private final ReviewService review;
    private final SwipeService swipes;
    private final PipelineService pipeline;
    private final ChatService chat;
    private final ProfileService profiles;
    private final SkillCatalog catalog;
    private final CollegeRepository colleges;
    private final AnalyticsDao analytics;

    public HirerController(JobService jobs, ReviewService review, SwipeService swipes,
                           PipelineService pipeline, ChatService chat, ProfileService profiles, SkillCatalog catalog,
                           CollegeRepository colleges, AnalyticsDao analytics) {
        this.jobs = jobs;
        this.review = review;
        this.swipes = swipes;
        this.pipeline = pipeline;
        this.chat = chat;
        this.profiles = profiles;
        this.catalog = catalog;
        this.colleges = colleges;
        this.analytics = analytics;
    }

    // ------------------------------------------------------------- dashboard & company

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal AppUserPrincipal me, Model model) {
        Hirer h = jobs.hirer(me.getId());
        model.addAttribute("hirer", h);
        model.addAttribute("company", h.getCompany());
        model.addAttribute("jobs", jobs.jobsOfCompany(h.getCompany().getId()));
        model.addAttribute("counters", analytics.jobCounters(h.getCompany().getId()));
        model.addAttribute("responsiveness", analytics.responsiveness(List.of(me.getId())).get(me.getId()));
        model.addAttribute("avgResponseHours", analytics.avgResponseHours(me.getId()));
        return "hirer/dashboard";
    }

    @GetMapping("/company")
    public String company(@AuthenticationPrincipal AppUserPrincipal me, Model model) {
        model.addAttribute("company", jobs.hirer(me.getId()).getCompany());
        return "hirer/company";
    }

    @PostMapping("/company")
    public String saveCompany(@AuthenticationPrincipal AppUserPrincipal me, @RequestParam(required = false) String website,
                              @RequestParam(required = false) String city, @RequestParam(required = false) String description,
                              RedirectAttributes flash) {
        jobs.updateCompany(me.getId(), website, city, description);
        flash.addFlashAttribute("flash", "Company profile saved");
        return "redirect:/hirer/company";
    }

    // ------------------------------------------------------------- jobs

    @GetMapping("/jobs/new")
    public String newJob(Model model) {
        return jobForm(model, new JobForm(), null);
    }

    @GetMapping("/jobs/{id}/edit")
    public String editJob(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable long id, Model model) {
        Job job = jobs.requireOwned(id, me.getId());
        return jobForm(model, JobForm.from(job), job);
    }

    private String jobForm(Model model, JobForm form, Job job) {
        model.addAttribute("form", form);
        model.addAttribute("job", job);
        model.addAttribute("skillsByCategory", catalog.byCategory());
        model.addAttribute("colleges", colleges.findByStatusOrderByNameAsc(VerificationStatus.VERIFIED));
        model.addAttribute("jobTypes", List.of(JobType.INTERNSHIP, JobType.FULL_TIME));
        model.addAttribute("workModes", List.of(WorkMode.REMOTE, WorkMode.HYBRID, WorkMode.ONSITE));
        return "hirer/job-form";
    }

    @PostMapping("/jobs")
    public String createJob(@AuthenticationPrincipal AppUserPrincipal me, @ModelAttribute JobForm form,
                            @RequestParam Map<String, String> params, RedirectAttributes flash) {
        collectWeights(form, params);
        Job job = jobs.create(me.getId(), form);
        flash.addFlashAttribute("flash", "Job \"" + job.getTitle() + "\" " + (form.isPublish() ? "published" : "saved as draft"));
        return "redirect:/hirer/dashboard";
    }

    @PostMapping("/jobs/{id}")
    public String updateJob(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable long id, @ModelAttribute JobForm form,
                            @RequestParam Map<String, String> params, RedirectAttributes flash) {
        collectWeights(form, params);
        jobs.update(id, me.getId(), form);
        flash.addFlashAttribute("flash", "Job updated");
        return "redirect:/hirer/dashboard";
    }

    /** Each skill row posts its weight as "weight_{skillId}". */
    private static void collectWeights(JobForm form, Map<String, String> params) {
        params.forEach((k, v) -> {
            if (k.startsWith("weight_")) {
                try {
                    form.getWeights().put(Long.parseLong(k.substring(7)), Integer.parseInt(v));
                } catch (NumberFormatException ignored) {
                    // a tampered field is simply ignored; the default weight applies
                }
            }
        });
    }

    @PostMapping("/jobs/{id}/status")
    public String jobStatus(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable long id,
                            @RequestParam JobStatus status, RedirectAttributes flash) {
        jobs.changeStatus(id, me.getId(), status);
        flash.addFlashAttribute("flash", "Job is now " + status);
        return "redirect:/hirer/dashboard";
    }

    // ------------------------------------------------------------- reviewing candidates

    @GetMapping("/jobs/{id}/queue")
    public String queue(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable long id, Model model) {
        return reviewPage(model, jobs.requireOwned(id, me.getId()), review.queue(id, me.getId()), HirerSwipeSource.QUEUE);
    }

    @GetMapping("/jobs/{id}/discover")
    public String discover(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable long id, Model model) {
        return reviewPage(model, jobs.requireOwned(id, me.getId()), review.discover(id, me.getId()), HirerSwipeSource.DISCOVER);
    }

    private String reviewPage(Model model, Job job, List<CandidateCard> cards, HirerSwipeSource source) {
        model.addAttribute("job", job);
        model.addAttribute("cards", cards);
        model.addAttribute("card", cards.isEmpty() ? null : cards.get(0));
        model.addAttribute("source", source);
        return "hirer/review";
    }

    @PostMapping("/jobs/{id}/swipe")
    public String swipe(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable long id, @RequestParam long applicantId,
                        @RequestParam SwipeDirection direction, @RequestParam HirerSwipeSource source,
                        RedirectAttributes flash) {
        boolean matched = swipes.hirerSwipe(me.getId(), id, applicantId, direction, source);
        if (matched) {
            flash.addFlashAttribute("matchFlash", "It's a match! The applicant can now apply with a tailored resume.");
        } else if (direction == SwipeDirection.RIGHT && source == HirerSwipeSource.DISCOVER) {
            flash.addFlashAttribute("flash", "Saved. They'll see that your company is interested when this job reaches their deck.");
        }
        return "redirect:/hirer/jobs/" + id + "/" + (source == HirerSwipeSource.QUEUE ? "queue" : "discover");
    }

    // ------------------------------------------------------------- pipeline

    @GetMapping("/jobs/{id}/applications")
    public String applications(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable long id, Model model) {
        model.addAttribute("job", jobs.requireOwned(id, me.getId()));
        model.addAttribute("applications", pipeline.ofJob(id, me.getId()));
        return "hirer/applications";
    }

    @GetMapping("/applications/{id}")
    public String application(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable long id, Model model) {
        Application app = pipeline.forHirer(id, me.getId());
        long applicantId = app.getMatch().getApplicant().getUserId();
        ScoreBreakdown score = review.explain(applicantId, app.getMatch().getJob().getId());
        model.addAttribute("app", app);
        model.addAttribute("job", app.getMatch().getJob());
        model.addAttribute("applicant", app.getMatch().getApplicant());
        model.addAttribute("score", score);
        model.addAttribute("matchedSkills", catalog.names(score.getMatchedSkillIds()));
        model.addAttribute("missingSkills", catalog.names(score.getMissingRequiredSkillIds()));
        model.addAttribute("resumeItems", profiles.resumeItems(applicantId));
        model.addAttribute("timeline", pipeline.timeline(id));
        model.addAttribute("slots", pipeline.slots(id));
        model.addAttribute("messages", chat.thread(app.getMatch().getId(), me.getId()));
        model.addAttribute("nextStatuses", app.getStatus().allowedNext().stream()
                .filter(s -> s == ApplicationStatus.SHORTLISTED || s == ApplicationStatus.INTERVIEW
                        || s == ApplicationStatus.OFFER || s == ApplicationStatus.REJECTED).toList());
        model.addAttribute("pdfReady", app.getTailoredPdfPath() != null && Files.exists(Path.of(app.getTailoredPdfPath())));
        return "hirer/application";
    }

    @PostMapping("/applications/{id}/status")
    public String move(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable long id,
                       @RequestParam ApplicationStatus status, @RequestParam(required = false) String note,
                       RedirectAttributes flash) {
        pipeline.hirerMove(me.getId(), id, status, note);
        flash.addFlashAttribute("flash", "Moved to " + status);
        return "redirect:/hirer/applications/" + id;
    }

    @PostMapping("/applications/{id}/slots")
    public String proposeSlots(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable long id,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime slot1,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime slot2,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime slot3,
                               @RequestParam(defaultValue = "30") int duration,
                               @RequestParam(required = false) String location, RedirectAttributes flash) {
        List<LocalDateTime> times = new ArrayList<>();
        times.add(slot1);
        times.add(slot2);
        times.add(slot3);
        pipeline.proposeSlots(me.getId(), id, times, duration, location);
        flash.addFlashAttribute("flash", "Interview slots sent to the applicant");
        return "redirect:/hirer/applications/" + id;
    }

    // ------------------------------------------------------------- analytics

    @GetMapping("/jobs/{id}/analytics")
    public String analytics(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable long id, Model model) {
        model.addAttribute("job", jobs.requireOwned(id, me.getId()));
        model.addAttribute("funnel", analytics.jobFunnel(id));
        model.addAttribute("skillGap", analytics.skillGap(id));
        return "hirer/analytics";
    }
}
