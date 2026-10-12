package com.swipehire.web.repository;

import com.swipehire.common.enums.NotificationType;
import com.swipehire.web.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findTop50ByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserIdAndReadAtIsNull(Long userId);

    boolean existsByUserIdAndTypeAndLink(Long userId, NotificationType type, String link);

    boolean existsByUserIdAndTypeAndLinkAndReadAtIsNull(Long userId, NotificationType type, String link);

    @Modifying
    @Query("update Notification n set n.readAt = :now where n.userId = :userId and n.readAt is null")
    int markAllRead(Long userId, LocalDateTime now);
}
