package com.halo.backend.service;

import com.halo.backend.factory.SensorDataFactory;
import com.halo.backend.model.HealthAlert;
import com.halo.backend.model.TelemetryData;
import com.halo.backend.observer.TelemetryMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import com.halo.backend.websocket.TelemetryWebSocketHandler;

@Service
public class TelemetryIngestionService {

    private static final Logger log = LoggerFactory.getLogger(TelemetryIngestionService.class);

    private final SensorDataFactory dataFactory;
    private final TelemetryMonitor telemetryMonitor;
    private final WebClient webClient;
    private final TelemetryWebSocketHandler webSocketHandler;
    
    // In-memory state of latest health scores
    private final Map<String, Double> latestHealthScores = new ConcurrentHashMap<>();
    
    // Reactive Sinks for streaming health scores per drone
    private final Map<String, reactor.core.publisher.Sinks.Many<Double>> healthScoreSinks = new ConcurrentHashMap<>();

    @Autowired
    public TelemetryIngestionService(SensorDataFactory dataFactory, 
                                     TelemetryMonitor telemetryMonitor,
                                     WebClient.Builder webClientBuilder,
                                     TelemetryWebSocketHandler webSocketHandler) {
        this.dataFactory = dataFactory;
        this.telemetryMonitor = telemetryMonitor;
        this.webClient = webClientBuilder.baseUrl("http://127.0.0.1:8000").build(); // Changed from 8002 to 8000
        this.webSocketHandler = webSocketHandler;
    }

    public void ingestData(List<Map<String, Object>> payloads) {
        // Java functional streams to process data
        payloads.stream()
            .map(dataFactory::createTelemetryData)
            .forEach(this::processTelemetry);
    }

    private void processTelemetry(TelemetryData data) {
        boolean hasAnomaly = Stream.of(
            checkAnomaly(data.getBatteryTemp() > 75.0, data, "CRITICAL", "Battery temperature exceeded 75.0C limit"),
            checkAnomaly(data.getMotorRpm() < 2000.0, data, "CRITICAL", "Motor RPM dropped below 2000.0 safe limit")
        ).anyMatch(a -> a);

        webClient.post().uri("/api/ai/rul").bodyValue(data).retrieve().bodyToMono(Map.class)
            .subscribe(
                response -> {
                    Object scoreObj = response.get("healthScore");
                    Object rulObj   = response.get("rul");
                    Double score = scoreObj instanceof Number ? ((Number) scoreObj).doubleValue() : null;
                    Double rul   = rulObj   instanceof Number ? ((Number) rulObj).doubleValue()   : null;
                    latestHealthScores.put(data.getDroneId(), score != null ? score : 100.0);
                    healthScoreSinks.computeIfAbsent(data.getDroneId(),
                            k -> reactor.core.publisher.Sinks.many().replay().latest())
                            .tryEmitNext(score != null ? score : 100.0);
                    data.setHealthScore(score);
                    data.setRul(rul);
                    webSocketHandler.broadcastTelemetry(data);
                    if (score != null && score < 50.0 && !hasAnomaly) {
                        triggerAlert(data, "MEDIUM", "ML Health Score dropped below 50");
                    }
                },
                error -> {
                    log.error("ML score error for drone {}: {}", data.getDroneId(), error.getMessage());
                    webSocketHandler.broadcastTelemetry(data);
                }
            );
    }

    private boolean checkAnomaly(boolean condition, TelemetryData data, String severity, String message) {
        if (condition) {
            triggerAlert(data, severity, message);
            return true;
        }
        return false;
    }

    private void triggerAlert(TelemetryData data, String severity, String message) {
        HealthAlert alert = HealthAlert.builder()
                .alertId(UUID.randomUUID().toString())
                .droneId(data.getDroneId())
                .timestamp(Instant.now().toString())
                .severity(severity)
                .message(message)
                .snapshot(data)
                .build();
        telemetryMonitor.notifyObservers(alert);
    }
    
    public Double getLatestHealthScore(String droneId) {
        return latestHealthScores.getOrDefault(droneId, 100.0);
    }
    
    public reactor.core.publisher.Flux<Double> getHealthScoreStream(String droneId) {
        return healthScoreSinks.computeIfAbsent(droneId, k -> reactor.core.publisher.Sinks.many().replay().latest()).asFlux();
    }
}
