package com.halo.backend.model;

public class DroneState {
    private String callsign;
    private String status;
    private double latitude;
    private double longitude;
    private double altitude;
    private double batteryTemp;
    private double motorRpm;
    private double vibrationScore;
    private double healthScore;
    private double rul;
    private String lastUpdated;

    public DroneState() {}

    public DroneState(String callsign, String status, double latitude, double longitude,
                      double altitude, double batteryTemp, double motorRpm,
                      double vibrationScore, double healthScore, double rul, String lastUpdated) {
        this.callsign = callsign;
        this.status = status;
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.batteryTemp = batteryTemp;
        this.motorRpm = motorRpm;
        this.vibrationScore = vibrationScore;
        this.healthScore = healthScore;
        this.rul = rul;
        this.lastUpdated = lastUpdated;
    }

    public String getCallsign() { return callsign; }
    public void setCallsign(String callsign) { this.callsign = callsign; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public double getAltitude() { return altitude; }
    public void setAltitude(double altitude) { this.altitude = altitude; }
    public double getBatteryTemp() { return batteryTemp; }
    public void setBatteryTemp(double batteryTemp) { this.batteryTemp = batteryTemp; }
    public double getMotorRpm() { return motorRpm; }
    public void setMotorRpm(double motorRpm) { this.motorRpm = motorRpm; }
    public double getVibrationScore() { return vibrationScore; }
    public void setVibrationScore(double vibrationScore) { this.vibrationScore = vibrationScore; }
    public double getHealthScore() { return healthScore; }
    public void setHealthScore(double healthScore) { this.healthScore = healthScore; }
    public double getRul() { return rul; }
    public void setRul(double rul) { this.rul = rul; }
    public String getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }
}
