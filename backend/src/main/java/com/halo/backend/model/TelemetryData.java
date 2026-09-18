package com.halo.backend.model;

public class TelemetryData {
    private String droneId;
    private String timestamp;
    private double batteryTemp;
    private double motorRpm;
    private double altitude;
    private double vibrationScore;
    
    // Extracted health score from ML microservice
    private Double healthScore;

    public TelemetryData() {}

    public TelemetryData(String droneId, String timestamp, double batteryTemp, double motorRpm, double altitude, double vibrationScore, Double healthScore) {
        this.droneId = droneId;
        this.timestamp = timestamp;
        this.batteryTemp = batteryTemp;
        this.motorRpm = motorRpm;
        this.altitude = altitude;
        this.vibrationScore = vibrationScore;
        this.healthScore = healthScore;
    }

    public String getDroneId() { return droneId; }
    public void setDroneId(String droneId) { this.droneId = droneId; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public double getBatteryTemp() { return batteryTemp; }
    public void setBatteryTemp(double batteryTemp) { this.batteryTemp = batteryTemp; }
    public double getMotorRpm() { return motorRpm; }
    public void setMotorRpm(double motorRpm) { this.motorRpm = motorRpm; }
    public double getAltitude() { return altitude; }
    public void setAltitude(double altitude) { this.altitude = altitude; }
    public double getVibrationScore() { return vibrationScore; }
    public void setVibrationScore(double vibrationScore) { this.vibrationScore = vibrationScore; }
    public Double getHealthScore() { return healthScore; }
    public void setHealthScore(Double healthScore) { this.healthScore = healthScore; }

    public static TelemetryDataBuilder builder() { return new TelemetryDataBuilder(); }

    public static class TelemetryDataBuilder {
        private String droneId;
        private String timestamp;
        private double batteryTemp;
        private double motorRpm;
        private double altitude;
        private double vibrationScore;
        private Double healthScore;
        public TelemetryDataBuilder droneId(String droneId) { this.droneId = droneId; return this; }
        public TelemetryDataBuilder timestamp(String timestamp) { this.timestamp = timestamp; return this; }
        public TelemetryDataBuilder batteryTemp(double batteryTemp) { this.batteryTemp = batteryTemp; return this; }
        public TelemetryDataBuilder motorRpm(double motorRpm) { this.motorRpm = motorRpm; return this; }
        public TelemetryDataBuilder altitude(double altitude) { this.altitude = altitude; return this; }
        public TelemetryDataBuilder vibrationScore(double vibrationScore) { this.vibrationScore = vibrationScore; return this; }
        public TelemetryDataBuilder healthScore(Double healthScore) { this.healthScore = healthScore; return this; }
        public TelemetryData build() { return new TelemetryData(droneId, timestamp, batteryTemp, motorRpm, altitude, vibrationScore, healthScore); }
    }
}
