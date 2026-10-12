package com.swipehire.web.websocket;

import jakarta.websocket.CloseReason;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;

/**
 * Jakarta WebSocket endpoint for live match alerts, notifications and chat messages.
 * It is push-only: the browser never sends anything here (sending a chat message is a normal
 * form POST), which keeps the JavaScript tiny and everything working without JS.
 */
@ServerEndpoint(value = "/ws/notify", configurator = HttpSessionConfigurator.class)
public class NotifyEndpoint {

    private Long userId;

    @OnOpen
    public void onOpen(Session session, EndpointConfig config) throws IOException {
        Object id = config.getUserProperties().get(HttpSessionConfigurator.USER_ID);
        if (!(id instanceof Long uid)) {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "login required"));
            return;
        }
        this.userId = uid;
        NotificationHub.register(uid, session);
    }

    @OnClose
    public void onClose(Session session) {
        if (userId != null) {
            NotificationHub.unregister(userId, session);
        }
    }

    @OnError
    public void onError(Session session, Throwable error) {
        onClose(session);
    }
}
