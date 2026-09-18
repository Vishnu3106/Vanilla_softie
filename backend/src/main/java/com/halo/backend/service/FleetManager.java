package com.halo.backend.service;

import com.halo.backend.model.DroneState;
import com.halo.backend.model.TelemetryData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@EnableScheduling
public class FleetManager {

    private static final double BASE_LAT    = 25.2048;
    private static final double BASE_LON    = 55.2708;
    private static final double COORD_SPREAD = 0.15;

    private final Map<String, DroneState> fleetRegistry   = new ConcurrentHashMap<>();
    private final Map<String, double[]>  trajectoryDelta  = new ConcurrentHashMap<>();
    private final TelemetryIngestionService ingestionService;
    private final Random random = new Random();

    @Autowired
    public FleetManager(TelemetryIngestionService ingestionService) {
        this.ingestionService = ingestionService;
        deployDrone("KESTREL-01");
        deployDrone("RAPTOR-02");
        deployDrone("FALCON-03");
    }

    public DroneState deployDrone(String callsign) {
        double startLat = BASE_LAT + (random.nextDouble() - 0.5) * COORD_SPREAD;
        double startLon = BASE_LON + (random.nextDouble() - 0.5) * COORD_SPREAD;

        DroneState state = new DroneState(
            callsign, "ACTIVE",
            startLat, startLon,
            100 + random.nextDouble() * 200,
            40 + random.nextDouble() * 20,
            2000 + random.nextDouble() * 500,
            random.nextDouble() * 0.3,
            100.0, 130.0,
            Instant.now().toString()
        );
        fleetRegistry.put(callsign, state);
        trajectoryDelta.put(callsign, new double[]{
            (random.nextDouble() - 0.5) * 0.0005,
            (random.nextDouble() - 0.5) * 0.0005
        });
        return state;
    }

    public DroneState overrideDrone(String callsign, String command) {
        DroneState state = fleetRegistry.get(callsign);
        if (state == null) return null;
        state.setStatus(command != null && !command.isBlank() ? command.toUpperCase() : "EMERGENCY_LAND");
        trajectoryDelta.put(callsign, new double[]{0.0, 0.0});
        return state;
    }

    public DroneState takeoffDrone(String callsign) {
        DroneState state = fleetRegistry.get(callsign);
        if (state == null) return null;
        state.setStatus("ACTIVE");
        state.setAltitude(80 + random.nextDouble() * 120);  // restore flight altitude
        state.setMotorRpm(2000 + random.nextDouble() * 500); // restore rotor speed
        // Reassign fresh random trajectory vector
        trajectoryDelta.put(callsign, new double[]{
            (random.nextDouble() - 0.5) * 0.0005,
            (random.nextDouble() - 0.5) * 0.0005
        });
        return state;
    }

    public Map<String, DroneState> getFleetRegistry() {
        return Collections.unmodifiableMap(fleetRegistry);
    }

    @Scheduled(fixedRate = 1000)
    public void simulateTelemetry() {
        if (fleetRegistry.isEmpty()) return;

        List<Map<String, Object>> payloads = new ArrayList<>();

        for (Map.Entry<String, DroneState> entry : fleetRegistry.entrySet()) {
            String callsign = entry.getKey();
            DroneState state = entry.getValue();

            if ("ACTIVE".equals(state.getStatus())) {
                // ── Active drone: update trajectory + physics ──────────────────
                double[] delta = trajectoryDelta.get(callsign);
                double newLat  = state.getLatitude()  + delta[0] + (random.nextDouble() - 0.5) * 0.0001;
                double newLon  = state.getLongitude() + delta[1] + (random.nextDouble() - 0.5) * 0.0001;

                if (Math.abs(newLat - BASE_LAT) > COORD_SPREAD / 2) delta[0] = -delta[0];
                if (Math.abs(newLon - BASE_LON) > COORD_SPREAD / 2) delta[1] = -delta[1];

                state.setLatitude(newLat);
                state.setLongitude(newLon);
                state.setAltitude(clamp(state.getAltitude()       + (random.nextDouble() - 0.5) * 5, 50, 500));
                state.setBatteryTemp(clamp(state.getBatteryTemp()  + (random.nextDouble() - 0.4) * 0.5, 35, 95));
                state.setMotorRpm(clamp(state.getMotorRpm()        + (random.nextDouble() - 0.5) * 50, 1200, 3000));
                state.setVibrationScore(clamp(state.getVibrationScore() + (random.nextDouble() - 0.5) * 0.02, 0, 1));
            } else {
                // ── Overridden drone: descend, freeze other sensors ────────────
                if (state.getAltitude() > 0) {
                    state.setAltitude(Math.max(0, state.getAltitude() - 5));
                }
                // Slow rotors and cool down
                state.setMotorRpm(clamp(state.getMotorRpm() - 20, 0, 3000));
                state.setBatteryTemp(clamp(state.getBatteryTemp() - 0.2, 20, 95));
            }

            // ── Always broadcast every drone so the frontend gets status updates ─
            state.setLastUpdated(Instant.now().toString());
            Map<String, Object> payload = new HashMap<>();
            payload.put("droneId",        callsign);
            payload.put("timestamp",      Instant.now().toString());
            payload.put("batteryTemp",    state.getBatteryTemp());
            payload.put("motorRpm",       state.getMotorRpm());
            payload.put("altitude",       state.getAltitude());
            payload.put("vibrationScore", state.getVibrationScore());
            payload.put("latitude",       state.getLatitude());
            payload.put("longitude",      state.getLongitude());
            payload.put("status",         state.getStatus());
            payloads.add(payload);
        }

        if (!payloads.isEmpty()) ingestionService.ingestData(payloads);
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}