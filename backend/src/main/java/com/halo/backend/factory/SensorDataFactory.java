package com.halo.backend.factory;

import com.halo.backend.model.TelemetryData;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SensorDataFactory {

    public TelemetryData createTelemetryData(Map<String, Object> rawData) {
        return TelemetryData.builder()
                .droneId((String) rawData.get("droneId"))
                .timestamp((String) rawData.get("timestamp"))
                .batteryTemp(parseDouble(rawData.get("batteryTemp")))
                .motorRpm(parseDouble(rawData.get("motorRpm")))
                .altitude(parseDouble(rawData.get("altitude")))
                .vibrationScore(parseDouble(rawData.get("vibrationScore")))
                .latitude(parseDouble(rawData.get("latitude")))
                .longitude(parseDouble(rawData.get("longitude")))
                .status(rawData.containsKey("status") ? (String) rawData.get("status") : "ACTIVE")
                .build();
    }

    private double parseDouble(Object val) {
        if (val == null) return 0.0;
        if (val instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(val.toString()); } catch (NumberFormatException e) { return 0.0; }
    }
}