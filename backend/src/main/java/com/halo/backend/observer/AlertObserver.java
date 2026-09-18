package com.halo.backend.observer;

import com.halo.backend.model.HealthAlert;

public interface AlertObserver {
    void onAlert(HealthAlert alert);
}
