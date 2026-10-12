package com.swipehire.web.controller;

import com.swipehire.web.dao.AnalyticsDao;
import com.swipehire.web.entity.Application;
import com.swipehire.web.security.AppUserPrincipal;
import com.swipehire.web.service.ChatService;
import com.swipehire.web.service.PipelineService;
import com.swipehire.web.service.ReviewService;
import com.swipehire.web.service.SkillCatalog;
import com.swipehire.web.service.TailoringService;
import com.swipehire.common.dto.ScoreBreakdown;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Files;
import java.nio.file.Path;

/** My Matches: the applicant's side of the pipeline, tailored resume, interview slots and chat. */
@Controller
@RequestMapping("/applicant")
public class ApplicantMatchController {

    private final PipelineService pipeline;
    private final ChatService chat;
    private final TailoringService tailoring;
    private final ReviewService review;
    private final SkillCatalog catalog;
    private final AnalyticsDao analytics;

    public ApplicantMatchController(PipelineService pipeline, ChatService chat, TailoringService tailoring,
                                    ReviewService review, SkillCatalog catalog, AnalyticsDao analytics) {
        this.pipeline = pipeline;
        this.chat = chat;
        this.tailoring = tailoring;
        this.review = review;
        this.catalog = catalog;
        this.analytics = analytics;
    }

    @GetMapping("/matches")
    public String matches(@AuthenticationPrincipal AppUserPrincipal me, Model model) {
        model.addAttribute("applications", pipeline.ofApplicant(me.getId()));
        return "applicant/matches";
    }

    @GetMapping("/matches/{id}")
    public String match(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable long id, Model model) {
        Application app = pipeline.forApplicant(id, me.getId());
        ScoreBreakdown score = review.explain(me.getId(), app.getMatch().getJob().getId());
        model.addAttribute("app", app);
        model.addAttribute("job", app.getMatch().getJob());
        model.addAttribute("score", score);
        model.addAttribute("matchedSkills", catalog.names(score.getMatchedSkillIds()));
        model.addAttribute("missingSkills", catalog.names(score.getMissingRequiredSkillIds()));
        model.addAttribute("timeline", pipeline.timeline(id));
        model.addAttribute("slots", pipeline.slots(id));
        model.addAttribute("messages", chat.thread(app.getMatch().getId(), me.getId()));
        model.addAttribute("tailoredItems", tailoring.preview(id));
        model.addAttribute("pdfReady", app.getTailoredPdfPath() != null && Files.exists(Path.of(app.getTailoredPdfPath())));
        return "applicant/match";
    }

    @PostMapping("/matches/{id}/apply")
    public String apply(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable long id, RedirectAttributes flash) {
        pipeline.apply(me.getId(), id);
        flash.addFlashAttribute("flash", "Applied! Your tailored resume was sent with the application.");
        return "redirect:/applicant/matches/" + id;
    }

    @PostMapping("/matches/{id}/withdraw")
    public String withdraw(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable long id, RedirectAttributes flash) {
        pipeline.withdraw(me.getId(), id);
        flash.addFlashAttribute("flash", "Application withdrawn");
        return "redirect:/applicant/matches/" + id;
    }

    @PostMapping("/matches/{id}/slots/{slotId}")
    public String chooseSlot(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable long id,
                             @PathVariable long slotId, RedirectAttributes flash) {
        pipeline.chooseSlot(me.getId(), id, slotId);
        flash.addFlashAttribute("flash", "Interview time confirmed");
        return "redirect:/applicant/matches/" + id;
    }

    /** Rebuilds the tailored PDF after the applicant edited their resume items. */
    @PostMapping("/matches/{id}/regenerate")
    public String regenerate(@AuthenticationPrincipal AppUserPrincipal me, @PathVariable long id, RedirectAttributes flash) {
        pipeline.forApplicant(id, me.getId());
        tailoring.generate(id);
        flash.addFlashAttribute("flash", "Tailored resume regenerated from your latest profile");
        return "redirect:/applicant/matches/" + id;
    }

    @GetMapping("/insights")
    public String insights(@AuthenticationPrincipal AppUserPrincipal me, Model model) {
        model.addAttribute("stats", analytics.applicantStats(me.getId()));
        model.addAttribute("missing", analytics.applicantMissingSkills(me.getId(), 8));
        return "applicant/insights";
    }
}
