package com.halo.backend.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.halo.backend.model.HealthAlert;
import com.halo.backend.model.TelemetryData;
import com.halo.backend.observer.AlertObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class TelemetryWebSocketHandler extends TextWebSocketHandler implements AlertObserver {

    private static final Logger log = LoggerFactory.getLogger(TelemetryWebSocketHandler.class);
    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("New WebSocket connection: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("WebSocket connection closed: {}", session.getId());
    }

    public void broadcastTelemetry(TelemetryData data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            TextMessage message = new TextMessage("{\"type\": \"TELEMETRY\", \"data\": " + json + "}");
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            }
        } catch (IOException e) {
            log.error("Failed to broadcast telemetry", e);
        }
    }

    @Override
    public void onAlert(HealthAlert alert) {
        try {
            String json = objectMapper.writeValueAsString(alert);
            TextMessage message = new TextMessage("{\"type\": \"ALERT\", \"data\": " + json + "}");
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            }
        } catch (IOException e) {
            log.error("Failed to broadcast alert", e);
        }
    }
}
