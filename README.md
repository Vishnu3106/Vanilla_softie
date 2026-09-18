# H.A.L.O.: Health & Aerial Logistics Observer

[![Java Version](https://img.shields.io/badge/Java-21-orange)](https://www.oracle.com/java/)
[![Python Version](https://img.shields.io/badge/Python-3.10%2B-blue)](https://www.python.org/)
[![React](https://img.shields.io/badge/React-Tailwind%20CSS-cyan)](https://reactjs.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)
[![UAV Compliance](https://img.shields.io/badge/Compliance-Simulated%20Part%20107-emerald)](#)

**H.A.L.O.** is a military-grade C4ISR (Command, Control, Communications, Computers, Intelligence, Surveillance, and Reconnaissance) tactical fleet management system. Designed to monitor and protect autonomous drone swarms, it fuses high-volume flight telemetry, predictive artificial intelligence, and multimodal visual feeds into a single Common Operating Picture (COP).

---

## 🏛️ System Architecture

```mermaid
graph TD
    %% Styling
    classDef darkHUD fill:#0f172a,stroke:#06b6d4,stroke-width:2px,color:#f8fafc;
    classDef dataBox fill:#1e293b,stroke:#94a3b8,stroke-width:1px,color:#e2e8f0;
    classDef highlight fill:#7f1d1d,stroke:#ef4444,stroke-width:2px,color:#fca5a5;

    %% Data Sources
    subgraph DataSources [Mission Datasets]
        DS1[OpenSky Telemetry <br> Trajectories]:::dataBox
        DS2[NASA CMAPSS <br> Predictive Data]:::dataBox
        DS3[VisDrone & AUAIR <br> Visual/Sensor Feeds]:::dataBox
    end

    %% Java Spring Boot Backend
    subgraph Backend [Tactical Ingestion Engine - Java 21 / Spring Boot :8080]
        Factory[SensorDataFactory <br> Data Normalization]:::darkHUD
        Streams[TelemetryIngestionService <br> Java Functional Streams]:::darkHUD
        Monitor[TelemetryMonitor <br> Subject]:::darkHUD
        Dispatcher[AlertDispatcher <br> Observer]:::highlight
        C2API[Command & Control APIs <br> /deploy, /override]:::darkHUD
        WS_Backend[WebSocket Gateway <br> State & Alerts]:::darkHUD
        
        DS1 -->|Live Flight Data| Factory
        Factory --> Streams
        Streams --> Monitor
        Monitor -->|Threshold Breach| Dispatcher
        Dispatcher -->|CRITICAL Alerts| WS_Backend
        Streams -->|Aggregated State| WS_Backend
        C2API -->|Injects/Modifies| Streams
    end

    %% Python FastAPI Microservice
    subgraph AIService [AI Operations Center - Python 3.10 / FastAPI :8000]
        XGB[XGBoost Engine <br> RUL Forecasting]:::darkHUD
        YOLO[YOLOv8 Vision Pipeline <br> Target Acquisition]:::highlight
        
        DS2 -->|Model Training| XGB
        DS3 -->|Raw Multimodal Frames| YOLO
        Streams -.->|Live Telemetry| XGB
        XGB -->|HealthScore & RUL| WS_Backend
    end

    %% React Frontend
    subgraph Frontend [C4ISR Tactical HUD - React / Tailwind CSS :3000]
        COP[Common Operating Picture <br> Leaflet Map]:::darkHUD
        Vitals[Vitals Dashboard <br> Chart.js]:::darkHUD
        ObsMatrix[Observer Matrix <br> Live Video Feed]:::darkHUD
        Console[Command Console <br> C2 Operations]:::highlight

        WS_Backend -->|Drone Coordinates| COP
        WS_Backend -->|Alerts & RUL Stats| Vitals
        YOLO -->|Annotated Bounding Box Stream| ObsMatrix
        Console -->|POST Commands| C2API
    end

🚀 Key Modules & Capabilities
1. ⚙️ Tactical Ingestion Engine (backend/)
High-Volume Telemetry Parsing: Utilizes Java functional streams to parse concurrent, simulated flight trajectories (ADS-B data) from the OpenSky dataset with ultra-low latency.

Observer Pattern Alerts: Continuously monitors metrics like motor RPM and battery temperature. Instantly dispatches CRITICAL warnings via WebSockets when physical thresholds are breached.

Command & Control (C2) APIs: Exposes REST endpoints (/api/fleet/deploy, /api/fleet/override) allowing operators to spawn new drones or force emergency landings.

2. 🧠 AI Operations Center (ml-service/)
Predictive Maintenance (RUL): An XGBoost model trained on the NASA CMAPSS dataset calculates the Remaining Useful Life (RUL) of drone rotors. It shifts the system from reactive to predictive by forecasting mechanical failures before they occur.

Multimodal Target Acquisition: A YOLOv8 computer vision pipeline processes synchronized visual/sensor feeds from the VisDrone and AUAIR datasets, drawing high-contrast bounding boxes around ground vehicles and personnel.

3. 💻 C4ISR Tactical HUD (frontend/)
Common Operating Picture (COP): A dark-mode, React-based interactive map using Leaflet.js to plot the live coordinates of the drone swarm.

Observer Matrix: A dedicated UI panel that streams the live, annotated video feed from the YOLOv8 pipeline for active threat verification.

Vitals Dashboard: Real-time Chart.js graphs displaying fleet-wide health scores and battery degradation trends.

📂 Repository Layout
halo-c4isr/
├── .github/
│   └── workflows/
│       └── build.yml              # CI/CD pipeline
├── data/
│   ├── 01_NASA_CMAPSS/            # Predictive maintenance training data
│   ├── 03_OpenSky/                # Flight trajectory CSVs
│   ├── 04_AUAIR/                  # Multimodal sensor feeds
│   └── 05_VisDrone/               # Surveillance visual data
├── backend/                       # Java 21 Spring Boot Application
│   ├── src/main/java/com/halo/
│   │   ├── controllers/           # C2 REST APIs & WebSocket handlers
│   │   ├── models/                # SensorDataFactory & Entities
│   │   └── services/              # TelemetryIngestionService (Java Streams)
│   └── pom.xml                    # Maven dependencies
├── ml-service/                    # Python FastAPI Microservice
│   ├── models/                    # Serialized XGBoost & YOLOv8 weights
│   ├── api.py                     # FastAPI endpoints (/predict, /ws/vision)
│   ├── train_rul.py               # CMAPSS model training script
│   └── requirements.txt           # Python dependencies
├── frontend/                      # React & Tailwind CSS Dashboard
│   ├── src/
│   │   ├── components/            # TacticalMap, VitalsChart, ObserverMatrix
│   │   └── App.tsx                # Main HUD layout
│   ├── tailwind.config.js         # Tactical UI color palette
│   └── package.json               # Node dependencies
└── README.md                      # Project documentation
