import React, { useState, useEffect, useRef, useCallback } from "react";
import { MapContainer, TileLayer, Marker, Popup, useMap, Polyline, Tooltip } from "react-leaflet";
import L from "leaflet";
import { Line } from "react-chartjs-2";
import {
  Chart as ChartJS, CategoryScale, LinearScale,
  PointElement, LineElement, Filler, Tooltip, Legend
} from "chart.js";

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Filler, Tooltip, Legend);

// Fix default Leaflet marker icons
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png",
  iconUrl:       "https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png",
  shadowUrl:     "https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png",
});

// ── Custom SVG drone icon factory ─────────────────────────────────────────────
const makeDroneIcon = (status) => {
  const color = status === "ACTIVE" ? "#00f0ff"
              : status === "EMERGENCY_LAND"  ? "#ff003c"
              : status === "RETURN_TO_BASE"  ? "#ffaa00"
              : "#888";
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="36" height="36" viewBox="0 0 36 36">
    <circle cx="18" cy="18" r="16" fill="#000" fill-opacity="0.7" stroke="${color}" stroke-width="2"/>
    <circle cx="18" cy="18" r="5" fill="${color}"/>
    <line x1="4"  y1="4"  x2="12" y2="12" stroke="${color}" stroke-width="1.5"/>
    <line x1="32" y1="4"  x2="24" y2="12" stroke="${color}" stroke-width="1.5"/>
    <line x1="4"  y1="32" x2="12" y2="24" stroke="${color}" stroke-width="1.5"/>
    <line x1="32" y1="32" x2="24" y2="24" stroke="${color}" stroke-width="1.5"/>
    <circle cx="5"  cy="5"  r="3" fill="none" stroke="${color}" stroke-width="1.5"/>
    <circle cx="31" cy="5"  r="3" fill="none" stroke="${color}" stroke-width="1.5"/>
    <circle cx="5"  cy="31" r="3" fill="none" stroke="${color}" stroke-width="1.5"/>
    <circle cx="31" cy="31" r="3" fill="none" stroke="${color}" stroke-width="1.5"/>
  </svg>`;
  return L.divIcon({
    html: svg,
    className: "",
    iconSize: [36, 36],
    iconAnchor: [18, 18],
    popupAnchor: [0, -20],
  });
};

// ── RUL Gauge Component ────────────────────────────────────────────────────────
const RulGauge = ({ value = 0, label = "" }) => {
  const pct = Math.min(100, Math.max(0, value));
  const color = pct > 70 ? "#00ff66" : pct > 30 ? "#ffaa00" : "#ff003c";
  return (
    <div className="rul-gauge">
      <div className="rul-label">{label}</div>
      <div className="rul-bar-bg">
        <div className="rul-bar-fill" style={{ width: `${pct}%`, background: color, boxShadow: `0 0 8px ${color}` }} />
      </div>
      <div className="rul-value" style={{ color }}>{pct.toFixed(1)}%</div>
    </div>
  );
};

// ── Deploy Drone Modal ─────────────────────────────────────────────────────────
const DeployModal = ({ onClose, onDeploy }) => {
  const [callsign, setCallsign] = useState("");
  const handleSubmit = (e) => {
    e.preventDefault();
    onDeploy(callsign.trim().toUpperCase() || undefined);
    onClose();
  };
  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-box" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <span className="modal-title">▶ DEPLOY NEW UNIT</span>
          <button className="modal-close" onClick={onClose}>✕</button>
        </div>
        <form onSubmit={handleSubmit}>
          <label className="modal-label">CALLSIGN (optional)</label>
          <input
            className="modal-input"
            placeholder="e.g. GHOST-07"
            value={callsign}
            onChange={(e) => setCallsign(e.target.value)}
            maxLength={12}
          />
          <button type="submit" className="btn btn-deploy">LAUNCH UNIT</button>
        </form>
      </div>
    </div>
  );
};

// ── Override Confirmation ──────────────────────────────────────────────────────
const OverrideConfirm = ({ droneId, command, onConfirm, onCancel }) => {
  const [count, setCount] = useState(3);
  // Use ref to avoid stale closure in the countdown effect
  const confirmRef = useRef(onConfirm);
  useEffect(() => { confirmRef.current = onConfirm; }, [onConfirm]);
  useEffect(() => {
    if (count <= 0) { confirmRef.current(); return; }
    const t = setTimeout(() => setCount(c => c - 1), 1000);
    return () => clearTimeout(t);
  }, [count]);
  return (
    <div className="modal-overlay">
      <div className="modal-box override-confirm">
        <div className="override-warn">⚠ COMMAND AUTHORITY REQUIRED</div>
        <div className="override-detail">{droneId} → {command}</div>
        <div className="override-countdown">Auto-executing in {count}s</div>
        <div className="override-actions">
          <button className="btn btn-danger" onClick={onConfirm}>EXECUTE NOW</button>
          <button className="btn btn-cancel" onClick={onCancel}>ABORT</button>
        </div>
      </div>
    </div>
  );
};

// ── Main App ───────────────────────────────────────────────────────────────────
const DUBAI = [25.2048, 55.2708];
const BACKEND_WS  = "ws://localhost:8081/ws/telemetry";
const ML_WS       = "ws://localhost:8000/ws/vision";
const BACKEND_URL = "http://localhost:8081";
const MAX_HISTORY = 30;

export default function App() {
  const [drones, setDrones]         = useState({});
  const [alerts, setAlerts]         = useState([]);
  const [selectedDrone, setSelected] = useState(null);
  const [showDeploy, setShowDeploy]  = useState(false);
  const [pendingOverride, setPending] = useState(null);
  const [batteryHistory, setBattery]  = useState({});
  const [dronePaths, setDronePaths]   = useState({});
  const [wsStatus, setWsStatus]       = useState("CONNECTING");

  // Vision WebSocket
  const [visionFrame, setVisionFrame] = useState(null);
  const [visionStatus, setVisionStatus] = useState("CONNECTING");
  const visionRef = useRef(null);

  // ── Telemetry WebSocket ─────────────────────────────────────────────────────
  useEffect(() => {
    let socket, retry;
    const connect = () => {
      socket = new WebSocket(BACKEND_WS);
      setWsStatus("CONNECTING");
      socket.onopen  = () => setWsStatus("LIVE");
      socket.onclose = () => { setWsStatus("RECONNECTING"); retry = setTimeout(connect, 2000); };
      socket.onmessage = (ev) => {
        const payload = JSON.parse(ev.data);
        if (payload.type === "TELEMETRY") {
          const d = payload.data;
          setDrones(prev => ({ ...prev, [d.droneId]: d }));
          setBattery(prev => {
            const hist = prev[d.droneId] || [];
            return { ...prev, [d.droneId]: [...hist, d.batteryTemp].slice(-MAX_HISTORY) };
          });
          setDronePaths(prev => {
            const path = prev[d.droneId] || [];
            // Only add point if we have valid coordinates
            if (d.latitude && d.longitude) {
              return { ...prev, [d.droneId]: [...path, [d.latitude, d.longitude]].slice(-100) };
            }
            return prev;
          });
        } else if (payload.type === "ALERT") {
          setAlerts(prev => [payload.data, ...prev].slice(0, 20));
        }
      };
    };
    connect();
    return () => { clearTimeout(retry); socket?.close(); };
  }, []);

  // ── Vision WebSocket ────────────────────────────────────────────────────────
  useEffect(() => {
    let ws, retry;
    const connect = () => {
      ws = new WebSocket(ML_WS);
      ws.binaryType = "arraybuffer";
      setVisionStatus("CONNECTING");
      ws.onopen  = () => setVisionStatus("LIVE");
      ws.onclose = () => {
        setVisionStatus("RECONNECTING");
        // Fallback to MJPEG after first disconnect
        retry = setTimeout(connect, 3000);
      };
      ws.onmessage = (ev) => {
        const blob = new Blob([ev.data], { type: "image/jpeg" });
        const url  = URL.createObjectURL(blob);
        setVisionFrame(prev => { if (prev) URL.revokeObjectURL(prev); return url; });
      };
      visionRef.current = ws;
    };
    connect();
    return () => { clearTimeout(retry); ws?.close(); };
  }, []);

  // ── API calls ───────────────────────────────────────────────────────────────
  const deployDrone = async (callsign) => {
    try {
      const body = callsign ? JSON.stringify({ callsign }) : "{}";
      const res  = await fetch(`${BACKEND_URL}/api/fleet/deploy`, {
        method: "POST", headers: { "Content-Type": "application/json" }, body
      });
      const data = await res.json();
      console.log("Deployed:", data);
    } catch (e) { console.error(e); }
  };

  const executeOverride = async (droneId, command) => {
    // Optimistic update — disable buttons immediately before WS echo
    setDrones(prev => ({
      ...prev,
      [droneId]: { ...prev[droneId], status: command }
    }));
    setPending(null);
    try {
      await fetch(`${BACKEND_URL}/api/fleet/${droneId}/override`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ command })
      });
    } catch (e) {
      console.error("Override failed:", e);
      // Revert optimistic update on error
      setDrones(prev => ({
        ...prev,
        [droneId]: { ...prev[droneId], status: "ACTIVE" }
      }));
    }
  };

  const executeTakeoff = async (droneId) => {
    // Optimistic update
    setDrones(prev => ({ ...prev, [droneId]: { ...prev[droneId], status: "ACTIVE" } }));
    try {
      await fetch(`${BACKEND_URL}/api/fleet/${droneId}/takeoff`, { method: "POST" });
    } catch (e) {
      console.error("Takeoff failed:", e);
    }
  };

  const requestOverride = (droneId, command) => setPending({ droneId, command });

  // ── Derived data ────────────────────────────────────────────────────────────
  const droneList    = Object.values(drones);
  const activeDrones = droneList.filter(d => d.status === "ACTIVE").length;
  const criticalDrones = droneList.filter(d => (d.healthScore ?? 100) < 30).length;
  const sel          = selectedDrone ? drones[selectedDrone] : droneList[0];

  // Chart data for selected drone
  const chartData = sel && batteryHistory[sel.droneId] ? {
    labels: batteryHistory[sel.droneId].map((_, i) => i),
    datasets: [{
      label: "Battery Temp (°C)",
      data: batteryHistory[sel.droneId],
      borderColor: "#00f0ff",
      backgroundColor: "rgba(0,240,255,0.08)",
      fill: true, tension: 0.4, pointRadius: 0, borderWidth: 2,
    }]
  } : null;

  const chartOptions = {
    responsive: true, maintainAspectRatio: false, animation: false,
    plugins: { legend: { display: false }, tooltip: { enabled: false } },
    scales: {
      x: { display: false },
      y: { grid: { color: "rgba(255,255,255,0.05)" }, ticks: { color: "#888", font: { size: 10 } } }
    }
  };

  return (
    <div className="halo-root">
      {/* ── HEADER ──────────────────────────────────────────────────────────── */}
      <header className="halo-header-bar">
        <div className="header-brand">
          <span className="brand-title">H.A.L.O.</span>
          <span className="brand-sub">HEALTH & AERIAL LOGISTICS OBSERVER // C4ISR v2.0</span>
        </div>
        <div className="header-stats">
          <div className="hstat"><span className="hstat-val">{droneList.length}</span><span className="hstat-lbl">UNITS</span></div>
          <div className="hstat active"><span className="hstat-val">{activeDrones}</span><span className="hstat-lbl">ACTIVE</span></div>
          <div className={`hstat ${criticalDrones > 0 ? "critical blink" : ""}`}>
            <span className="hstat-val">{criticalDrones}</span><span className="hstat-lbl">CRITICAL</span>
          </div>
          <div className={`ws-pill ${wsStatus === "LIVE" ? "live" : "dead"}`}>{wsStatus}</div>
        </div>
        <button className="btn btn-deploy hdr-deploy" onClick={() => setShowDeploy(true)}>⊕ DEPLOY</button>
      </header>

      {/* ── MAIN GRID ───────────────────────────────────────────────────────── */}
      <div className="halo-grid">

        {/* COL 1: COP MAP */}
        <section className="panel map-panel">
          <div className="panel-title">◈ COMMON OPERATING PICTURE</div>
          <MapContainer center={DUBAI} zoom={13} className="leaflet-fill" zoomControl={false}>
            <TileLayer
              attribution='&copy; <a href="https://carto.com/">CartoDB</a>'
              url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
            />
            {droneList.map(d => (
              <React.Fragment key={d.droneId}>
                {dronePaths[d.droneId] && (
                  <Polyline 
                    positions={dronePaths[d.droneId]} 
                    color={d.status === "ACTIVE" ? "#00f0ff" : "#ff003c"} 
                    weight={2} 
                    opacity={0.6} 
                    dashArray="4 4" 
                  />
                )}
                <Marker
                  position={[d.latitude ?? DUBAI[0], d.longitude ?? DUBAI[1]]}
                  icon={makeDroneIcon(d.status)}
                >
                  <Tooltip direction="top" offset={[0, -20]} opacity={1} className="halo-tooltip">
                    <div className="tooltip-content">
                      <div className="tt-title">{d.droneId}</div>
                      <div className="tt-row"><span>STATUS:</span><span style={{color: d.status === "ACTIVE" ? "#00f0ff" : "#ff003c"}}>{d.status}</span></div>
                      <div className="tt-row"><span>LAT:</span><span>{d.latitude?.toFixed(5)}</span></div>
                      <div className="tt-row"><span>LON:</span><span>{d.longitude?.toFixed(5)}</span></div>
                    </div>
                  </Tooltip>
                  <Popup className="cop-popup">
                  <div className="popup-inner">
                    <div className="popup-title">{d.droneId}</div>
                    <div className="popup-row"><span>STATUS</span><span style={{ color: d.status === "ACTIVE" ? "#00f0ff" : "#ff003c" }}>{d.status}</span></div>
                    <div className="popup-row"><span>ALT</span><span>{d.altitude?.toFixed(1)} m</span></div>
                    <div className="popup-row"><span>TEMP</span><span className={d.batteryTemp > 75 ? "txt-red" : ""}>{d.batteryTemp?.toFixed(1)}°C</span></div>
                    <div className="popup-row"><span>RPM</span><span className={d.motorRpm < 2000 ? "txt-amber" : ""}>{d.motorRpm?.toFixed(0)}</span></div>
                    <div className="popup-row"><span>RUL</span><span>{d.rul?.toFixed(1) ?? "—"}</span></div>
                    <RulGauge value={d.healthScore ?? 0} label="HEALTH" />
                  </div>
                </Popup>
              </Marker>
              </React.Fragment>
            ))}
          </MapContainer>
          <div className="map-overlay-badge">LIVE TRACKING</div>
        </section>

        {/* COL 2: VITALS + COMMAND */}
        <div className="col2">
          {/* FLEET VITALS */}
          <section className="panel vitals-panel">
            <div className="panel-title">◈ FLEET VITALS</div>
            <div className="drone-tabs">
              {droneList.map(d => (
                <button
                  key={d.droneId}
                  className={`drone-tab ${sel?.droneId === d.droneId ? "active" : ""}`}
                  onClick={() => setSelected(d.droneId)}
                  style={{ borderColor: d.status !== "ACTIVE" ? "#ff003c" : undefined }}
                >
                  {d.droneId.length > 10 ? d.droneId.slice(0, 10) : d.droneId}
                </button>
              ))}
            </div>
            {sel ? (
              <div className="vitals-body">
                <RulGauge value={sel.healthScore ?? 0} label={`${sel.droneId} — HEALTH SCORE`} />
                <div className="vitals-row">
                  <div className="vstat">
                    <div className="vstat-label">BATTERY TEMP</div>
                    <div className={`vstat-value ${sel.batteryTemp > 75 ? "txt-red" : ""}`}>{sel.batteryTemp?.toFixed(1)}°C</div>
                  </div>
                  <div className="vstat">
                    <div className="vstat-label">MOTOR RPM</div>
                    <div className={`vstat-value ${sel.motorRpm < 2000 ? "txt-amber" : ""}`}>{sel.motorRpm?.toFixed(0)}</div>
                  </div>
                  <div className="vstat">
                    <div className="vstat-label">ALTITUDE</div>
                    <div className="vstat-value">{sel.altitude?.toFixed(0)} m</div>
                  </div>
                  <div className="vstat">
                    <div className="vstat-label">RUL CYCLES</div>
                    <div className="vstat-value">{sel.rul?.toFixed(1) ?? "—"}</div>
                  </div>
                </div>
                {chartData && (
                  <div className="chart-container">
                    <div className="chart-title">BATTERY DEGRADATION TREND</div>
                    <div className="chart-wrap">
                      <Line data={chartData} options={chartOptions} />
                    </div>
                  </div>
                )}
              </div>
            ) : <div className="no-data">NO UNITS ONLINE</div>}
          </section>

          {/* COMMAND CONSOLE */}
          <section className="panel cmd-panel">
            <div className="panel-title">◈ COMMAND CONSOLE</div>
            <div className="cmd-list">
              {droneList.length === 0 && <div className="no-data">Deploy units to enable C2</div>}
              {droneList.map(d => (
                <div key={d.droneId} className="cmd-row">
                  <div className="cmd-callsign">{d.droneId}</div>
                  <div className={`status-pill ${d.status === "ACTIVE" ? "pill-active" : "pill-override"}`}>{d.status}</div>
                  <div className="cmd-btns">
                    <button
                      className="btn btn-deploy"
                      onClick={() => executeTakeoff(d.droneId)}
                      disabled={d.status === "ACTIVE"}
                      style={{ padding: "4px 8px", fontSize: "0.75rem", marginRight: "6px" }}
                    >TAKEOFF</button>
                    <button
                      className="btn btn-override-sm"
                      onClick={() => requestOverride(d.droneId, "EMERGENCY_LAND")}
                      disabled={d.status !== "ACTIVE"}
                    >LAND</button>
                    <button
                      className="btn btn-rtb-sm"
                      onClick={() => requestOverride(d.droneId, "RETURN_TO_BASE")}
                      disabled={d.status !== "ACTIVE"}
                    >RTB</button>
                  </div>
                </div>
              ))}
            </div>
          </section>
        </div>

        {/* COL 3: OBSERVER + ALERTS */}
        <div className="col3">
          {/* OBSERVER MATRIX */}
          <section className="panel observer-panel">
            <div className="panel-title">
              ◈ OBSERVER MATRIX
              <span className={`ws-pill sm ${visionStatus === "LIVE" ? "live" : "dead"}`}>{visionStatus}</span>
            </div>
            <div className="observer-feed">
              {visionFrame ? (
                <img src={visionFrame} className="observer-img" alt="YOLOv8 annotated feed" />
              ) : (
                <img
                  src="http://127.0.0.1:8000/api/ai/vision"
                  className="observer-img"
                  alt="MJPEG fallback feed"
                  onError={(e) => { e.target.style.display = "none"; }}
                />
              )}
              <div className="observer-hud">
                <div className="observer-corner tl" />
                <div className="observer-corner tr" />
                <div className="observer-corner bl" />
                <div className="observer-corner br" />
                <div className="observer-scan" />
              </div>
              <div className="observer-label">LIVE: AI TARGET ACQUISITION</div>
            </div>
          </section>

          {/* ALERT FEED */}
          <section className="panel alert-panel">
            <div className="panel-title txt-red">⚠ CRITICAL ALERTS</div>
            <div className="alert-scroll">
              {alerts.length === 0 && <div className="no-data">No active alerts</div>}
              {alerts.map((a, i) => (
                <div key={i} className={`alert-row ${a.severity === "CRITICAL" ? "alert-critical" : "alert-medium"}`}>
                  <div className="alert-head">
                    <span className="alert-drone">{a.droneId}</span>
                    <span className={`sev-badge sev-${a.severity?.toLowerCase()}`}>{a.severity}</span>
                  </div>
                  <div className="alert-msg">{a.message}</div>
                  <div className="alert-ts">{a.timestamp?.split("T")[1]?.split(".")[0]}</div>
                </div>
              ))}
            </div>
          </section>
        </div>
      </div>

      {/* ── MODALS ──────────────────────────────────────────────────────────── */}
      {showDeploy && <DeployModal onClose={() => setShowDeploy(false)} onDeploy={deployDrone} />}
      {pendingOverride && (
        <OverrideConfirm
          droneId={pendingOverride.droneId}
          command={pendingOverride.command}
          onConfirm={() => executeOverride(pendingOverride.droneId, pendingOverride.command)}
          onCancel={() => setPending(null)}
        />
      )}
    </div>
  );
}