package com.halo.backend.controller;

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
    public ResponseEntity<Map<String, String>> deployDrone() {
        String callsign = "DRONE-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        fleetManager.deployDrone(callsign);
        
        Map<String, String> response = new HashMap<>();
        response.put("callsign", callsign);
        response.put("status", "DEPLOYED");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{droneId}/override")
    public ResponseEntity<Map<String, String>> overrideDrone(@PathVariable String droneId) {
        fleetManager.overrideDrone(droneId);
        
        Map<String, String> response = new HashMap<>();
        response.put("droneId", droneId);
        response.put("status", "OVERRIDE_ACTIVATED");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getFleetStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("activeDrones", fleetManager.getFleetStatus().size());
        response.put("drones", fleetManager.getFleetStatus());
        return ResponseEntity.ok(response);
    }
}
