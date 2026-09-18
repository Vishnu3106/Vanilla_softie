package com.halo.backend.observer;

import com.halo.backend.model.HealthAlert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TelemetryMonitor {

    private final List<AlertObserver> observers = new ArrayList<>();

    @Autowired
    public TelemetryMonitor(List<AlertObserver> alertObservers) {
        this.observers.addAll(alertObservers);
    }

    public void addObserver(AlertObserver observer) {
        observers.add(observer);
    }

    public void notifyObservers(HealthAlert alert) {
        for (AlertObserver observer : observers) {
            observer.onAlert(alert);
        }
    }
}
