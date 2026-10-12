package com.swipehire.web.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Who is connected right now: userId -> their open WebSocket sessions (one per browser tab).
 * <p>
 * The container creates a new endpoint object per connection and calls it from many threads,
 * so the registry is a static ConcurrentHashMap with concurrent sets as values. Sending on one
 * session is synchronised because a WebSocket session may not write two messages at once.
 */
public final class NotificationHub {

    private static final Logger log = LoggerFactory.getLogger(NotificationHub.class);
    private static final Map<Long, Set<Session>> SESSIONS = new ConcurrentHashMap<>();
    private static final ObjectMapper JSON = new ObjectMapper();

    private NotificationHub() {
    }

    static void register(long userId, Session session) {
        SESSIONS.computeIfAbsent(userId, id -> ConcurrentHashMap.newKeySet()).add(session);
    }

    static void unregister(long userId, Session session) {
        Set<Session> set = SESSIONS.get(userId);
        if (set != null) {
            set.remove(session);
            if (set.isEmpty()) {
                SESSIONS.remove(userId, set);
            }
        }
    }

    public static int onlineUsers() {
        return SESSIONS.size();
    }

    /** Sends a JSON message to every open tab of the user. Silently skips users who are offline. */
    public static void push(long userId, Map<String, ?> payload) {
        Set<Session> set = SESSIONS.get(userId);
        if (set == null || set.isEmpty()) {
            return;
        }
        String text;
        try {
            text = JSON.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
        for (Session s : set) {
            if (!s.isOpen()) {
                unregister(userId, s);
                continue;
            }
            synchronized (s) {
                try {
                    s.getBasicRemote().sendText(text);
                } catch (IOException e) {
                    log.debug("WebSocket send failed for user {}: {}", userId, e.getMessage());
                    unregister(userId, s);
                }
            }
        }
    }
}
