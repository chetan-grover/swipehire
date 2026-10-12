package com.swipehire.web.repository;

import com.swipehire.web.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByMatchIdOrderBySentAtAsc(Long matchId);

    @Modifying
    @Query("update Message m set m.readAt = :now where m.matchId = :matchId and m.sender.id <> :readerId and m.readAt is null")
    int markRead(Long matchId, Long readerId, LocalDateTime now);
}
