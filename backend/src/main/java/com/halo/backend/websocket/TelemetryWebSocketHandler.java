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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TelemetryWebSocketHandler extends TextWebSocketHandler implements AlertObserver {

    private static final Logger log = LoggerFactory.getLogger(TelemetryWebSocketHandler.class);

    // Per-session lock map -- prevents concurrent sendMessage on the same session
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, Object>           locks    = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
        locks.put(session.getId(), new Object());
        log.info("New WebSocket connection: {}", session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        locks.remove(session.getId());
        log.info("WebSocket connection closed: {}", session.getId());
    }

    private void safeSend(WebSocketSession session, TextMessage message) {
        Object lock = locks.get(session.getId());
        if (lock == null) return;
        synchronized (lock) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(message);
                } catch (IOException e) {
                    log.warn("Send failed for session {}: {}", session.getId(), e.getMessage());
                }
            }
        }
    }

    public void broadcastTelemetry(TelemetryData data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            TextMessage message = new TextMessage("{\"type\": \"TELEMETRY\", \"data\": " + json + "}");
            sessions.values().forEach(s -> safeSend(s, message));
        } catch (IOException e) {
            log.error("Failed to serialize telemetry", e);
        }
    }

    @Override
    public void onAlert(HealthAlert alert) {
        try {
            String json = objectMapper.writeValueAsString(alert);
            TextMessage message = new TextMessage("{\"type\": \"ALERT\", \"data\": " + json + "}");
            sessions.values().forEach(s -> safeSend(s, message));
        } catch (IOException e) {
            log.error("Failed to serialize alert", e);
        }
    }
}