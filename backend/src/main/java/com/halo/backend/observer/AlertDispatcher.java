package com.halo.backend.observer;

import com.halo.backend.model.HealthAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@Component
public class AlertDispatcher implements AlertObserver {
    
    private static final Logger log = LoggerFactory.getLogger(AlertDispatcher.class);
    
    // Reactive Sink that replays the last 100 alerts to any new subscribers
    private final Sinks.Many<HealthAlert> alertSink = Sinks.many().replay().limit(100);

    @Override
    public void onAlert(HealthAlert alert) {
        log.error(">>> HEALTH ALERT TRIGGERED <<<");
        log.error("Drone: {} | Severity: {} | Message: {}", alert.getDroneId(), alert.getSeverity(), alert.getMessage());
        
        // Emit the alert to the reactive stream
        alertSink.tryEmitNext(alert);
    }
    
    public Flux<HealthAlert> getAlertStream() {
        return alertSink.asFlux();
    }
}
