package com.swipehire.web.service;

import com.swipehire.common.enums.ApplicationStatus;
import com.swipehire.common.enums.NotificationType;
import com.swipehire.common.enums.Role;
import com.swipehire.web.config.AppTime;
import com.swipehire.web.entity.Application;
import com.swipehire.web.entity.Hirer;
import com.swipehire.web.entity.JobMatch;
import com.swipehire.web.entity.Message;
import com.swipehire.web.entity.User;
import com.swipehire.web.repository.ApplicationRepository;
import com.swipehire.web.repository.HirerRepository;
import com.swipehire.web.repository.MessageRepository;
import com.swipehire.web.repository.UserRepository;
import com.swipehire.web.support.BusinessException;
import com.swipehire.web.support.NotFoundException;
import com.swipehire.web.websocket.NotificationHub;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Chat inside a match. Sending is a normal form POST (works without JavaScript); the WebSocket
 * only pushes the new message to the other person's open page.
 */
@Service
public class ChatService {

    static final int MAX_LENGTH = 2000;
    private static final Set<ApplicationStatus> CLOSED = EnumSet.of(ApplicationStatus.EXPIRED, ApplicationStatus.WITHDRAWN);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("d MMM, h:mm a");

    private final MessageRepository messages;
    private final ApplicationRepository applications;
    private final HirerRepository hirers;
    private final UserRepository users;
    private final NotificationService notifications;

    public ChatService(MessageRepository messages, ApplicationRepository applications, HirerRepository hirers,
                       UserRepository users, NotificationService notifications) {
        this.messages = messages;
        this.applications = applications;
        this.hirers = hirers;
        this.users = users;
        this.notifications = notifications;
    }

    @Transactional
    public List<Message> thread(long matchId, long readerId) {
        requireParticipant(matchId, readerId);
        messages.markRead(matchId, readerId, AppTime.now());
        return messages.findByMatchIdOrderBySentAtAsc(matchId);
    }

    @Transactional
    public void send(long matchId, long senderId, String body) {
        Application app = requireParticipant(matchId, senderId);
        if (CLOSED.contains(app.getStatus())) {
            throw new BusinessException("This conversation is closed");
        }
        String text = body == null ? "" : body.trim();
        if (text.isEmpty() || text.length() > MAX_LENGTH) {
            throw new BusinessException("Message must be 1-" + MAX_LENGTH + " characters");
        }
        User sender = users.findById(senderId).orElseThrow();
        Message msg = messages.save(new Message(matchId, sender, text));

        JobMatch match = app.getMatch();
        boolean fromApplicant = sender.getRole() == Role.APPLICANT;
        long recipientId = fromApplicant ? match.getJob().getPostedBy() : match.getApplicant().getUserId();
        String link = (fromApplicant ? "/hirer/applications/" : "/applicant/matches/") + app.getId();

        Map<String, Object> payload = Map.of("type", "chat", "matchId", matchId, "sender", sender.getFullName(),
                "body", text, "sentAt", msg.getSentAt().format(TIME));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                NotificationHub.push(recipientId, payload);
            }
        });
        // One unread "new message" notification per conversation is enough; don't spam the bell.
        if (!notifications.hasUnread(recipientId, NotificationType.MESSAGE, link)) {
            notifications.notify(recipientId, NotificationType.MESSAGE, "New message from " + sender.getFullName(),
                    text.length() > 80 ? text.substring(0, 80) + "…" : text, link);
        }
    }

    /** The applicant of the match, or any recruiter of the job's company, may read and write. */
    private Application requireParticipant(long matchId, long userId) {
        Application app = applications.findByMatchId(matchId).orElseThrow(() -> new NotFoundException("Conversation not found"));
        JobMatch m = app.getMatch();
        boolean isApplicant = m.getApplicant().getUserId().equals(userId);
        boolean isHirer = hirers.findById(userId)
                .map(Hirer::getCompany).map(c -> c.getId().equals(m.getJob().getCompany().getId())).orElse(false);
        if (!isApplicant && !isHirer) {
            throw new NotFoundException("Conversation not found");
        }
        return app;
    }
}
