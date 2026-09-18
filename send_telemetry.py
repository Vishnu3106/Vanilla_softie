import requests
import time
import random
import pandas as pd
from datetime import datetime
import math

url = "http://localhost:8081/api/telemetry/ingest"

# Load OpenSky dataset
try:
    df = pd.read_csv("data/unzipped/OpenSky/03_OpenSky_flight_telemetry/opensky_trajectories.csv")
    df = df.dropna(subset=['callsign', 'velocity', 'baro_altitude', 'vertical_rate'])
    # Pick a few distinct "drones" (aircrafts)
    target_drones = df['callsign'].unique()[:3]
    df = df[df['callsign'].isin(target_drones)]
except Exception as e:
    print(f"Error loading OpenSky dataset: {e}")
    exit(1)

print(f"Streaming data for drones: {target_drones}")

# Group by time to simulate streaming
timestamps = df['snapshot_time'].unique()
timestamps.sort()

while True:
    for t in timestamps:
        current_data = df[df['snapshot_time'] == t]
        payloads = []
        
        for _, row in current_data.iterrows():
            # Occasionally inject a huge anomaly for the predictive maintenance model
            is_anomaly = random.random() < 0.1
            
            # Map OpenSky features to our Telemetry schema:
            drone_id = str(row['callsign']).strip()
            velocity = float(row['velocity'])
            altitude = float(row['baro_altitude'])
            vertical_rate = float(row['vertical_rate'])
            
            # Synthesize battery temperature based on velocity
            base_temp = 50 + (velocity / 10)
            battery_temp = 90.0 if is_anomaly else base_temp
            
            # Synthesize RPM based on velocity
            motor_rpm = 1800 if is_anomaly else (velocity * 20 + random.uniform(100, 300))
            
            # Synthesize vibration from vertical rate
            vibration = abs(vertical_rate) + random.uniform(2, 5)
            
            payload = {
                "droneId": drone_id,
                "timestamp": datetime.utcnow().isoformat() + "Z",
                "batteryTemp": battery_temp,
                "motorRpm": motor_rpm,
                "altitude": altitude,
                "vibrationScore": vibration
            }
            payloads.append(payload)
        
        if payloads:
            try:
                response = requests.post(url, json=payloads)
                print(f"Sent batch for {len(payloads)} drones, Status: {response.status_code}")
            except Exception as e:
                print(f"Failed to send telemetry: {e}")
        
        time.sleep(2)
    print("Restarting telemetry loop...")
