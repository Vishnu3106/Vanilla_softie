package com.halo.backend.controller;

import com.halo.backend.model.HealthAlert;
import com.halo.backend.observer.AlertDispatcher;
import com.halo.backend.service.TelemetryIngestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allow frontend to connect
public class TelemetryController {

    private final TelemetryIngestionService ingestionService;
    private final AlertDispatcher alertDispatcher;

    @Autowired
    public TelemetryController(TelemetryIngestionService ingestionService, AlertDispatcher alertDispatcher) {
        this.ingestionService = ingestionService;
        this.alertDispatcher = alertDispatcher;
    }

    @PostMapping("/telemetry/ingest")
    public ResponseEntity<String> ingestTelemetry(@RequestBody List<Map<String, Object>> payloads) {
        ingestionService.ingestData(payloads);
        return ResponseEntity.ok("Telemetry ingested successfully");
    }

    @GetMapping("/health/{droneId}")
    public ResponseEntity<Double> getHealthScore(@PathVariable String droneId) {
        return ResponseEntity.ok(ingestionService.getLatestHealthScore(droneId));
    }
    
    @GetMapping(value = "/health/stream/{droneId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Double> streamHealthScore(@PathVariable String droneId) {
        return ingestionService.getHealthScoreStream(droneId);
    }

    @GetMapping(value = "/alerts/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<HealthAlert> streamAllAlerts() {
        return alertDispatcher.getAlertStream();
    }
}
