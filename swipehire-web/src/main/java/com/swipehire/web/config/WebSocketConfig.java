package com.swipehire.web.config;

import com.swipehire.web.websocket.NotifyEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * With embedded Tomcat, @ServerEndpoint classes are not scanned automatically; this exporter
 * registers them with Tomcat's Jakarta WebSocket container. The container (not Spring) then
 * creates one NotifyEndpoint object per connection.
 */
@Configuration
public class WebSocketConfig {

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        ServerEndpointExporter exporter = new ServerEndpointExporter();
        exporter.setAnnotatedEndpointClasses(NotifyEndpoint.class);
        return exporter;
    }
}
