package com.halo.backend.service;

import com.halo.backend.controller.CommandController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@EnableScheduling
public class FleetManager {

    private final Map<String, String> fleetStatus = new ConcurrentHashMap<>();
    private final TelemetryIngestionService ingestionService;
    private final Random random = new Random();

    @Autowired
    public FleetManager(TelemetryIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    public void deployDrone(String callsign) {
        fleetStatus.put(callsign, "ACTIVE");
    }

    public void overrideDrone(String callsign) {
        fleetStatus.put(callsign, "EMERGENCY_OVERRIDE");
    }

    public Map<String, String> getFleetStatus() {
        return fleetStatus;
    }

    @Scheduled(fixedRate = 1000)
    public void simulateTelemetry() {
        if (fleetStatus.isEmpty()) return;

        List<Map<String, Object>> payloads = new ArrayList<>();
        
        for (Map.Entry<String, String> entry : fleetStatus.entrySet()) {
            String droneId = entry.getKey();
            String status = entry.getValue();
            
            if ("EMERGENCY_OVERRIDE".equals(status)) {
                continue; // Stop sending telemetry if overridden
            }
            
            // Generate some random simulated telemetry
            Map<String, Object> data = new HashMap<>();
            data.put("droneId", droneId);
            data.put("timestamp", Instant.now().toString());
            data.put("batteryTemp", 40.0 + random.nextDouble() * 40.0); // 40-80
            data.put("motorRpm", 1500.0 + random.nextDouble() * 1000.0); // 1500-2500
            data.put("altitude", 100.0 + random.nextDouble() * 200.0);
            data.put("vibrationScore", random.nextDouble());
            
            payloads.add(data);
        }
        
        if (!payloads.isEmpty()) {
            ingestionService.ingestData(payloads);
        }
    }
}
