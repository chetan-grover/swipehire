package com.swipehire.web.service;

import com.swipehire.common.enums.ApplicationStatus;
import com.swipehire.common.enums.NotificationType;
import com.swipehire.web.config.AppTime;
import com.swipehire.web.entity.Application;
import com.swipehire.web.entity.ApplicationStatusHistory;
import com.swipehire.web.entity.InterviewSlot;
import com.swipehire.web.entity.Job;
import com.swipehire.web.entity.User;
import com.swipehire.web.repository.ApplicationRepository;
import com.swipehire.web.repository.ApplicationStatusHistoryRepository;
import com.swipehire.web.repository.InterviewSlotRepository;
import com.swipehire.web.repository.UserRepository;
import com.swipehire.web.support.BusinessException;
import com.swipehire.web.support.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The application pipeline. Every status change goes through {@link #transition}, which checks the
 * state machine (ApplicationStatus.canMoveTo) and writes a history row, so the audit trail is complete.
 */
@Service
public class PipelineService {

    public static final Duration EXPIRY = Duration.ofDays(7);
    static final Set<ApplicationStatus> HIRER_TARGETS = EnumSet.of(ApplicationStatus.SHORTLISTED,
            ApplicationStatus.INTERVIEW, ApplicationStatus.OFFER, ApplicationStatus.REJECTED);
    private static final DateTimeFormatter SLOT_FMT = DateTimeFormatter.ofPattern("EEE d MMM, h:mm a");

    private final ApplicationRepository applications;
    private final ApplicationStatusHistoryRepository history;
    private final InterviewSlotRepository slots;
    private final UserRepository users;
    private final JobService jobService;
    private final NotificationService notifications;
    private final EmailService email;

    public PipelineService(ApplicationRepository applications, ApplicationStatusHistoryRepository history,
                           InterviewSlotRepository slots, UserRepository users, JobService jobService,
                           NotificationService notifications, EmailService email) {
        this.applications = applications;
        this.history = history;
        this.slots = slots;
        this.users = users;
        this.jobService = jobService;
        this.notifications = notifications;
        this.email = email;
    }

    // ------------------------------------------------------------- access checks

    @Transactional(readOnly = true)
    public Application forApplicant(long applicationId, long applicantId) {
        return applications.findById(applicationId)
                .filter(a -> a.getMatch().getApplicant().getUserId().equals(applicantId))
                .orElseThrow(() -> new NotFoundException("Application not found"));
    }

    @Transactional(readOnly = true)
    public Application forHirer(long applicationId, long hirerId) {
        Application app = applications.findById(applicationId).orElseThrow(() -> new NotFoundException("Application not found"));
        jobService.requireOwned(app.getMatch().getJob().getId(), hirerId);
        return app;
    }

    @Transactional(readOnly = true)
    public List<Application> ofApplicant(long applicantId) {
        return applications.findForApplicant(applicantId);
    }

    @Transactional(readOnly = true)
    public List<Application> ofJob(long jobId, long hirerId) {
        jobService.requireOwned(jobId, hirerId);
        return applications.findForJob(jobId);
    }

    @Transactional(readOnly = true)
    public List<ApplicationStatusHistory> timeline(long applicationId) {
        return history.findTimeline(applicationId);
    }

    @Transactional(readOnly = true)
    public List<InterviewSlot> slots(long applicationId) {
        return slots.findByApplicationIdOrderBySlotStartAsc(applicationId);
    }

    // ------------------------------------------------------------- applicant actions

    /** One-click apply: the tailored resume is attached automatically. */
    @Transactional
    public void apply(long applicantId, long applicationId) {
        Application app = forApplicant(applicationId, applicantId);
        transition(app, ApplicationStatus.APPLIED, users.getReferenceById(applicantId), "Applied with tailored resume");
        app.setAppliedAt(AppTime.now());
        Job job = app.getMatch().getJob();
        notifications.notify(job.getPostedBy(), NotificationType.STATUS_CHANGE,
                app.getMatch().getApplicant().getUser().getFullName() + " applied for " + job.getTitle(),
                "Please respond within 7 days.", "/hirer/applications/" + app.getId());
    }

    @Transactional
    public void withdraw(long applicantId, long applicationId) {
        Application app = forApplicant(applicationId, applicantId);
        transition(app, ApplicationStatus.WITHDRAWN, users.getReferenceById(applicantId), "Withdrawn by applicant");
        Job job = app.getMatch().getJob();
        notifications.notify(job.getPostedBy(), NotificationType.STATUS_CHANGE,
                app.getMatch().getApplicant().getUser().getFullName() + " withdrew from " + job.getTitle(),
                null, "/hirer/applications/" + app.getId());
    }

    @Transactional
    public void chooseSlot(long applicantId, long applicationId, long slotId) {
        Application app = forApplicant(applicationId, applicantId);
        if (app.getStatus() != ApplicationStatus.INTERVIEW) {
            throw new BusinessException("Interview slots can only be picked at the interview stage");
        }
        List<InterviewSlot> all = slots.findByApplicationIdOrderBySlotStartAsc(applicationId);
        InterviewSlot chosen = all.stream().filter(s -> s.getId().equals(slotId)).findFirst()
                .orElseThrow(() -> new NotFoundException("Slot not found"));
        all.forEach(s -> s.setChosen(s == chosen));
        Job job = app.getMatch().getJob();
        notifications.notify(job.getPostedBy(), NotificationType.INTERVIEW,
                app.getMatch().getApplicant().getUser().getFullName() + " picked " + chosen.getSlotStart().format(SLOT_FMT),
                job.getTitle(), "/hirer/applications/" + app.getId());
    }

    // ------------------------------------------------------------- hirer actions

    @Transactional
    public void hirerMove(long hirerId, long applicationId, ApplicationStatus to, String note) {
        if (!HIRER_TARGETS.contains(to)) {
            throw new BusinessException("Hirers can only shortlist, invite to interview, offer or reject");
        }
        Application app = forHirer(applicationId, hirerId);
        transition(app, to, users.getReferenceById(hirerId), note);
        User applicant = app.getMatch().getApplicant().getUser();
        Job job = app.getMatch().getJob();
        String title = switch (to) {
            case SHORTLISTED -> "You're shortlisted for " + job.getTitle() + "!";
            case INTERVIEW -> job.getCompany().getName() + " wants to interview you";
            case OFFER -> "Offer from " + job.getCompany().getName() + "!";
            default -> "Update on " + job.getTitle() + " at " + job.getCompany().getName();
        };
        notifications.notify(applicant.getId(), NotificationType.STATUS_CHANGE, title, note,
                "/applicant/matches/" + app.getId());
        email.sendAsync(applicant.getEmail(), title, title + (note == null || note.isBlank() ? "" : "\n\n" + note)
                + "\n\nOpen SwipeHire for details.");
    }

    /** Hirer proposes up to 3 interview times; replaces any earlier proposals. */
    @Transactional
    public void proposeSlots(long hirerId, long applicationId, List<LocalDateTime> times, int durationMin, String location) {
        Application app = forHirer(applicationId, hirerId);
        if (app.getStatus() != ApplicationStatus.INTERVIEW) {
            throw new BusinessException("Move the application to INTERVIEW before proposing slots");
        }
        List<LocalDateTime> valid = times.stream().filter(t -> t != null && t.isAfter(AppTime.now())).distinct().limit(3).toList();
        if (valid.isEmpty()) {
            throw new BusinessException("Propose at least one future time");
        }
        slots.deleteAllForApplication(applicationId);
        int duration = Math.max(10, Math.min(240, durationMin));
        valid.forEach(t -> slots.save(new InterviewSlot(applicationId, t, duration, AccountService.trimToNull(location))));
        notifications.notify(app.getMatch().getApplicant().getUserId(), NotificationType.INTERVIEW,
                "Pick an interview time for " + app.getMatch().getJob().getTitle(),
                valid.size() + " slot(s) proposed", "/applicant/matches/" + app.getId());
    }

    // ------------------------------------------------------------- system actions

    /**
     * Hourly job: applications untouched for 7 days in MATCHED (applicant never applied) or
     * APPLIED (hirer never responded) become EXPIRED. changed_by stays NULL = "system".
     */
    @Transactional
    public int expireStale() {
        List<Application> stale = applications.findStale(EnumSet.of(ApplicationStatus.MATCHED, ApplicationStatus.APPLIED),
                AppTime.now().minus(EXPIRY));
        for (Application app : stale) {
            ApplicationStatus before = app.getStatus();
            transition(app, ApplicationStatus.EXPIRED, null,
                    before == ApplicationStatus.MATCHED ? "Applicant did not apply within 7 days"
                            : "Hirer did not respond within 7 days");
            Job job = app.getMatch().getJob();
            notifications.notify(app.getMatch().getApplicant().getUserId(), NotificationType.STATUS_CHANGE,
                    "Your match for " + job.getTitle() + " expired", null, "/applicant/matches/" + app.getId());
            notifications.notify(job.getPostedBy(), NotificationType.STATUS_CHANGE,
                    "Match expired: " + app.getMatch().getApplicant().getUser().getFullName(), job.getTitle(),
                    "/hirer/applications/" + app.getId());
        }
        return stale.size();
    }

    /** Warns whoever has to act, once, when a match has less than 24 hours left. */
    @Transactional
    public int sendExpiryWarnings() {
        LocalDateTime warnFrom = AppTime.now().minus(EXPIRY).plusDays(1);
        int sent = 0;
        for (Application app : applications.findStale(EnumSet.of(ApplicationStatus.MATCHED, ApplicationStatus.APPLIED), warnFrom)) {
            boolean hirerTurn = app.getStatus() == ApplicationStatus.APPLIED;
            long userId = hirerTurn ? app.getMatch().getJob().getPostedBy() : app.getMatch().getApplicant().getUserId();
            String link = (hirerTurn ? "/hirer/applications/" : "/applicant/matches/") + app.getId();
            if (!notifications.alreadySent(userId, NotificationType.EXPIRY_WARNING, link)) {
                notifications.notify(userId, NotificationType.EXPIRY_WARNING,
                        "Less than 24 hours left on " + app.getMatch().getJob().getTitle(),
                        hirerTurn ? "Respond to the applicant before the match expires." : "Apply before the match expires.",
                        link);
                sent++;
            }
        }
        return sent;
    }

    private void transition(Application app, ApplicationStatus to, User actor, String note) {
        ApplicationStatus from = app.getStatus();
        if (!from.canMoveTo(to)) {
            throw new BusinessException("Can't move an application from " + from + " to " + to);
        }
        app.setStatus(to);
        history.save(new ApplicationStatusHistory(app, from, to, actor, AccountService.trimToNull(note)));
    }
}
