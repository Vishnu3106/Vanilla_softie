package com.halo.backend.model;

import java.time.Instant;

public class HealthAlert {
    private String alertId;
    private String droneId;
    private String timestamp;
    private String severity; // CRITICAL, MEDIUM, LOW
    private String message;
    private TelemetryData snapshot;

    public HealthAlert() {}

    public HealthAlert(String alertId, String droneId, String timestamp, String severity, String message, TelemetryData snapshot) {
        this.alertId = alertId;
        this.droneId = droneId;
        this.timestamp = timestamp;
        this.severity = severity;
        this.message = message;
        this.snapshot = snapshot;
    }

    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }
    public String getDroneId() { return droneId; }
    public void setDroneId(String droneId) { this.droneId = droneId; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public TelemetryData getSnapshot() { return snapshot; }
    public void setSnapshot(TelemetryData snapshot) { this.snapshot = snapshot; }

    public static HealthAlertBuilder builder() { return new HealthAlertBuilder(); }

    public static class HealthAlertBuilder {
        private String alertId;
        private String droneId;
        private String timestamp;
        private String severity;
        private String message;
        private TelemetryData snapshot;
        public HealthAlertBuilder alertId(String alertId) { this.alertId = alertId; return this; }
        public HealthAlertBuilder droneId(String droneId) { this.droneId = droneId; return this; }
        public HealthAlertBuilder timestamp(String timestamp) { this.timestamp = timestamp; return this; }
        public HealthAlertBuilder severity(String severity) { this.severity = severity; return this; }
        public HealthAlertBuilder message(String message) { this.message = message; return this; }
        public HealthAlertBuilder snapshot(TelemetryData snapshot) { this.snapshot = snapshot; return this; }
        public HealthAlert build() { return new HealthAlert(alertId, droneId, timestamp, severity, message, snapshot); }
    }
}
