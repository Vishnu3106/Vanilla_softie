package com.halo.backend.model;

public class TelemetryData {
    private String droneId;
    private String timestamp;
    private double batteryTemp;
    private double motorRpm;
    private double altitude;
    private double vibrationScore;
    private double latitude;
    private double longitude;
    private String status;
    private Double healthScore;
    private Double rul;

    public TelemetryData() {}

    public String getDroneId() { return droneId; }
    public void setDroneId(String v) { this.droneId = v; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String v) { this.timestamp = v; }
    public double getBatteryTemp() { return batteryTemp; }
    public void setBatteryTemp(double v) { this.batteryTemp = v; }
    public double getMotorRpm() { return motorRpm; }
    public void setMotorRpm(double v) { this.motorRpm = v; }
    public double getAltitude() { return altitude; }
    public void setAltitude(double v) { this.altitude = v; }
    public double getVibrationScore() { return vibrationScore; }
    public void setVibrationScore(double v) { this.vibrationScore = v; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double v) { this.latitude = v; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double v) { this.longitude = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public Double getHealthScore() { return healthScore; }
    public void setHealthScore(Double v) { this.healthScore = v; }
    public Double getRul() { return rul; }
    public void setRul(Double v) { this.rul = v; }

    public static TelemetryDataBuilder builder() { return new TelemetryDataBuilder(); }

    public static class TelemetryDataBuilder {
        private String droneId, timestamp, status;
        private double batteryTemp, motorRpm, altitude, vibrationScore, latitude, longitude;
        private Double healthScore, rul;
        public TelemetryDataBuilder droneId(String v) { this.droneId = v; return this; }
        public TelemetryDataBuilder timestamp(String v) { this.timestamp = v; return this; }
        public TelemetryDataBuilder batteryTemp(double v) { this.batteryTemp = v; return this; }
        public TelemetryDataBuilder motorRpm(double v) { this.motorRpm = v; return this; }
        public TelemetryDataBuilder altitude(double v) { this.altitude = v; return this; }
        public TelemetryDataBuilder vibrationScore(double v) { this.vibrationScore = v; return this; }
        public TelemetryDataBuilder latitude(double v) { this.latitude = v; return this; }
        public TelemetryDataBuilder longitude(double v) { this.longitude = v; return this; }
        public TelemetryDataBuilder status(String v) { this.status = v; return this; }
        public TelemetryDataBuilder healthScore(Double v) { this.healthScore = v; return this; }
        public TelemetryDataBuilder rul(Double v) { this.rul = v; return this; }
        public TelemetryData build() {
            TelemetryData d = new TelemetryData();
            d.droneId = droneId; d.timestamp = timestamp; d.batteryTemp = batteryTemp;
            d.motorRpm = motorRpm; d.altitude = altitude; d.vibrationScore = vibrationScore;
            d.latitude = latitude; d.longitude = longitude; d.status = status;
            d.healthScore = healthScore; d.rul = rul;
            return d;
        }
    }
}