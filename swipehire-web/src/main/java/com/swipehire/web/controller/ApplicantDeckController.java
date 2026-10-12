package com.swipehire.web.controller;

import com.swipehire.common.enums.SwipeDirection;
import com.swipehire.web.dao.QuotaDao;
import com.swipehire.web.security.AppUserPrincipal;
import com.swipehire.web.service.DeckService;
import com.swipehire.web.service.ProfileService;
import com.swipehire.web.service.SwipeService;
import com.swipehire.web.view.JobCard;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/** The swipe deck. Works with plain form buttons; the JS drag gesture just submits the same forms. */
@Controller
@RequestMapping("/applicant")
public class ApplicantDeckController {

    private final DeckService deck;
    private final SwipeService swipes;
    private final ProfileService profiles;

    public ApplicantDeckController(DeckService deck, SwipeService swipes, ProfileService profiles) {
        this.deck = deck;
        this.swipes = swipes;
        this.profiles = profiles;
    }

    @GetMapping("/deck")
    public String deck(@AuthenticationPrincipal AppUserPrincipal me, Model model) {
        List<JobCard> cards = deck.cards(me.getId(), 3);
        int[] used = swipes.quotaUsage(me.getId());
        model.addAttribute("cards", cards);
        model.addAttribute("card", cards.isEmpty() ? null : cards.get(0));
        model.addAttribute("rightLeft", QuotaDao.RIGHT_LIMIT - used[0]);
        model.addAttribute("superLeft", QuotaDao.SUPER_LIMIT - used[1]);
        model.addAttribute("remaining", deck.remaining(me.getId()));
        model.addAttribute("checklist", profiles.checklist(me.getId()));
        return "applicant/deck";
    }

    @PostMapping("/swipe")
    public String swipe(@AuthenticationPrincipal AppUserPrincipal me, @RequestParam long jobId,
                        @RequestParam SwipeDirection direction, @RequestParam(required = false) String pitch,
                        RedirectAttributes flash) {
        boolean matched;
        try {
            matched = swipes.applicantSwipe(me.getId(), jobId, direction, pitch);
        } finally {
            // Re-rank on a background thread while the browser follows the redirect.
            deck.refreshInBackground(me.getId());
        }
        if (matched) {
            flash.addFlashAttribute("matchFlash", "It's a match! The hirer had already liked you. Check My Matches.");
        }
        flash.addFlashAttribute("canUndo", true);
        return "redirect:/applicant/deck";
    }

    @PostMapping("/swipe/undo")
    public String undo(@AuthenticationPrincipal AppUserPrincipal me, RedirectAttributes flash) {
        swipes.undoLast(me.getId());
        deck.refreshInBackground(me.getId());
        flash.addFlashAttribute("flash", "Last swipe undone");
        return "redirect:/applicant/deck";
    }
}
