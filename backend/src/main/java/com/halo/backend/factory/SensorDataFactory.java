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
                .build();
    }
    
    private double parseDouble(Object val) {
        if (val == null) return 0.0;
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        try {
            return Double.parseDouble(val.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
