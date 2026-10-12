package com.swipehire.web.websocket;

import com.swipehire.web.security.SecurityConfig;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.HandshakeResponse;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 * Runs during the HTTP -> WebSocket upgrade. The upgrade request still carries the login
 * session cookie, so we can read the user id that the login handler stored in the HttpSession.
 * (Tomcat gives every connection its own copy of the config, so userProperties are per-connection.)
 */
public class HttpSessionConfigurator extends ServerEndpointConfig.Configurator {

    static final String USER_ID = "userId";

    @Override
    public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request, HandshakeResponse response) {
        HttpSession httpSession = (HttpSession) request.getHttpSession();
        if (httpSession != null) {
            Object userId = httpSession.getAttribute(SecurityConfig.SESSION_USER_ID);
            if (userId != null) {
                config.getUserProperties().put(USER_ID, userId);
            }
        }
    }
}
