package com.swipehire.web.service;

import com.swipehire.common.enums.NotificationType;
import com.swipehire.web.config.AppTime;
import com.swipehire.web.entity.Notification;
import com.swipehire.web.repository.NotificationRepository;
import com.swipehire.web.websocket.NotificationHub;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;

/** Stores a notification (bell icon) and pushes it live over the WebSocket. */
@Service
public class NotificationService {

    private final NotificationRepository repo;

    public NotificationService(NotificationRepository repo) {
        this.repo = repo;
    }

    /**
     * REQUIRES_NEW: runs in its own transaction, because it is also called from after-commit
     * listeners where the original transaction is already finished. The live push is sent only
     * after this transaction commits, so a click on the toast always finds the row.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notify(long userId, NotificationType type, String title, String body, String link) {
        repo.save(new Notification(userId, type, title, body, link));
        Map<String, String> payload = Map.of("type", "notify", "kind", type.name(), "title", title,
                "link", link == null ? "" : link);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                NotificationHub.push(userId, payload);
            }
        });
    }

    @Transactional(readOnly = true)
    public List<Notification> recent(long userId) {
        return repo.findTop50ByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public long unreadCount(long userId) {
        return repo.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public void markAllRead(long userId) {
        repo.markAllRead(userId, AppTime.now());
    }

    @Transactional(readOnly = true)
    public boolean alreadySent(long userId, NotificationType type, String link) {
        return repo.existsByUserIdAndTypeAndLink(userId, type, link);
    }

    @Transactional(readOnly = true)
    public boolean hasUnread(long userId, NotificationType type, String link) {
        return repo.existsByUserIdAndTypeAndLinkAndReadAtIsNull(userId, type, link);
    }
}
