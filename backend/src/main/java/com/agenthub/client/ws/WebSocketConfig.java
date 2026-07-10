package com.agenthub.client.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final SessionChatWebSocketHandler sessionChatWebSocketHandler;

    public WebSocketConfig(SessionChatWebSocketHandler sessionChatWebSocketHandler) {
        this.sessionChatWebSocketHandler = sessionChatWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(sessionChatWebSocketHandler, "/ws/sessions")
                .setAllowedOrigins("*");
    }
}
