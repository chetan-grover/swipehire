package com.swipehire.web.service;

import com.swipehire.common.enums.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Side effects of a new match. AFTER_COMMIT guarantees we never alert anyone about a match that
 * was rolled back. Slow work (email, PDF) runs on background threads.
 */
@Component
public class MatchEventListener {

    private static final Logger log = LoggerFactory.getLogger(MatchEventListener.class);

    private final NotificationService notifications;
    private final EmailService email;
    private final TailoringService tailoring;

    public MatchEventListener(NotificationService notifications, EmailService email, TailoringService tailoring) {
        this.notifications = notifications;
        this.email = email;
        this.tailoring = tailoring;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMatch(MatchCreatedEvent e) {
        log.info("New match #{}: {} <-> {} ({})", e.matchId(), e.applicantName(), e.jobTitle(), e.companyName());
        notifications.notify(e.applicantId(), NotificationType.MATCH,
                "It's a match! " + e.companyName() + " liked you back for " + e.jobTitle(),
                "Your tailored resume is being prepared. Review it and apply in one click.",
                "/applicant/matches/" + e.applicationId());
        notifications.notify(e.hirerId(), NotificationType.MATCH,
                "New match: " + e.applicantName() + " for " + e.jobTitle(),
                "The application opens once they apply. You have 7 days to respond after that.",
                "/hirer/applications/" + e.applicationId());

        email.sendAsync(e.applicantEmail(), "It's a match: " + e.jobTitle() + " at " + e.companyName(),
                "Hi " + e.applicantName() + ",\n\n" + e.companyName() + " is interested in you for " + e.jobTitle()
                        + ".\nOpen SwipeHire to review your tailored resume and apply in one click.\n\n- SwipeHire");
        email.sendAsync(e.hirerEmail(), "New match for " + e.jobTitle(),
                e.applicantName() + " matched with your job " + e.jobTitle() + ".\n\n- SwipeHire");

        tailoring.generateAsync(e.applicationId());
    }
}
