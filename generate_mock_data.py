import pandas as pd
import numpy as np
import os

os.makedirs('data', exist_ok=True)

# Generate Predictive Maintenance Data
np.random.seed(42)
n_samples = 1000

drone_ids = [f'DRONE-{i:03d}' for i in range(1, 11)]

data = {
    'droneId': np.random.choice(drone_ids, n_samples),
    'timestamp': pd.date_range(start='2026-09-01', periods=n_samples, freq='10min'),
    'batteryTemp': np.random.normal(60, 10, n_samples), # Temp in C, >75 is anomaly
    'motorRpm': np.random.normal(5000, 1000, n_samples), # RPM, <2000 is anomaly
    'altitude': np.random.normal(120, 20, n_samples),
    'vibrationScore': np.random.normal(10, 2, n_samples)
}

df = pd.DataFrame(data)

# Inject anomalies and define target 'healthScore'
# Health score formula: 100 - (anomalies impact)
df['healthScore'] = 100.0
anomaly_idx = (df['batteryTemp'] > 75) | (df['motorRpm'] < 2000)
df.loc[anomaly_idx, 'healthScore'] = np.random.uniform(20, 60, size=anomaly_idx.sum())
df.loc[~anomaly_idx, 'healthScore'] = np.random.uniform(85, 100, size=(~anomaly_idx).sum())

df.to_csv('data/predictive_maintenance.csv', index=False)
df.to_csv('data/flight_telemetry.csv', index=False)

print("Mock datasets generated successfully in data/")
