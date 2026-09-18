package com.halo.backend.controller;

import com.halo.backend.model.DroneState;
import com.halo.backend.service.FleetManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/fleet")
@CrossOrigin(origins = "*")
public class CommandController {

    private final FleetManager fleetManager;

    @Autowired
    public CommandController(FleetManager fleetManager) {
        this.fleetManager = fleetManager;
    }

    @PostMapping("/deploy")
    public ResponseEntity<DroneState> deployDrone(@RequestBody(required = false) Map<String, String> body) {
        String callsign = (body != null && body.containsKey("callsign") && !body.get("callsign").isBlank())
            ? body.get("callsign").toUpperCase()
            : "UAV-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        DroneState state = fleetManager.deployDrone(callsign);
        return ResponseEntity.ok(state);
    }

    @PostMapping("/{droneId}/override")
    public ResponseEntity<Map<String, Object>> overrideDrone(
            @PathVariable String droneId,
            @RequestBody(required = false) Map<String, String> body) {
        String command = (body != null) ? body.getOrDefault("command", "EMERGENCY_LAND") : "EMERGENCY_LAND";
        DroneState updated = fleetManager.overrideDrone(droneId, command);

        Map<String, Object> response = new HashMap<>();
        response.put("droneId", droneId);
        response.put("command", command);
        response.put("status", updated != null ? updated.getStatus() : "NOT_FOUND");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{droneId}/takeoff")
    public ResponseEntity<Map<String, Object>> takeoffDrone(@PathVariable String droneId) {
        DroneState updated = fleetManager.takeoffDrone(droneId);
        Map<String, Object> response = new HashMap<>();
        response.put("droneId", droneId);
        response.put("command", "TAKEOFF");
        response.put("status", updated != null ? updated.getStatus() : "NOT_FOUND");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getFleetStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("activeDrones", fleetManager.getFleetRegistry().size());
        response.put("drones", fleetManager.getFleetRegistry());
        return ResponseEntity.ok(response);
    }
}