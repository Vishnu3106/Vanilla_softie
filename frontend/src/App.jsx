import React, { useState, useEffect } from 'react';
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import L from 'leaflet';

// Fix Leaflet marker icons
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
  iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
  iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
  shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
});

function App() {
  const [drones, setDrones] = useState({});
  const [alerts, setAlerts] = useState([]);
  const [ws, setWs] = useState(null);

  useEffect(() => {
    const socket = new WebSocket('ws://localhost:8081/ws/telemetry');
    setWs(socket);

    socket.onmessage = (event) => {
      const payload = JSON.parse(event.data);
      if (payload.type === 'TELEMETRY') {
        setDrones(prev => ({
          ...prev,
          [payload.data.droneId]: payload.data
        }));
      } else if (payload.type === 'ALERT') {
        setAlerts(prev => [payload.data, ...prev].slice(0, 10));
      }
    };

    return () => socket.close();
  }, []);

  const deployDrone = async () => {
    try {
      const res = await fetch('http://localhost:8081/api/fleet/deploy', { method: 'POST' });
      const data = await res.json();
      console.log('Deployed:', data);
    } catch (e) {
      console.error(e);
    }
  };

  const overrideDrone = async (droneId) => {
    try {
      await fetch(`http://localhost:8081/api/fleet/${droneId}/override`, { method: 'POST' });
    } catch (e) {
      console.error(e);
    }
  };

  // Center on Dubai for static map
  const position = [25.2048, 55.2708]; 

  return (
    <div className="h-screen w-screen flex flex-col p-4 space-y-4">
      <header className="flex justify-between items-center border-b border-[var(--cyan-accent)] pb-4">
        <div>
          <h1 className="text-3xl font-bold text-[var(--cyan-accent)] tracking-widest">H.A.L.O.</h1>
          <p className="text-sm text-gray-400">Health & Aerial Logistics Observer</p>
        </div>
        <button 
          onClick={deployDrone}
          className="bg-[var(--surface)] border border-[var(--cyan-accent)] px-6 py-2 text-[var(--cyan-accent)] hover:bg-[var(--cyan-accent)] hover:text-black transition-colors"
        >
          DEPLOY DRONE
        </button>
      </header>

      <div className="flex-1 grid grid-cols-3 gap-4 min-h-0">
        {/* Left Column: Map & Vitals */}
        <div className="col-span-2 flex flex-col space-y-4">
          <div className="flex-1 bg-[var(--surface)] border border-[var(--grid-cyan)] relative overflow-hidden p-1">
            <MapContainer center={position} zoom={13} style={{ height: '100%', width: '100%' }}>
              <TileLayer
                attribution='&copy; <a href="https://carto.com/">CartoDB</a>'
                url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
              />
              {Object.values(drones).map(d => (
                <Marker key={d.droneId} position={[position[0] + (d.altitude - 100)*0.0001, position[1] + d.vibrationScore*0.0001]}>
                  <Popup>
                    <div className="text-black">
                      <b>{d.droneId}</b><br/>
                      Alt: {d.altitude.toFixed(1)}m<br/>
                      RUL: {d.healthScore ? d.healthScore.toFixed(2) : 'N/A'}
                    </div>
                  </Popup>
                </Marker>
              ))}
            </MapContainer>
            <div className="absolute top-2 left-2 z-[1000] bg-black/50 p-2 text-xs border border-[var(--cyan-accent)] text-[var(--cyan-accent)]">
              TACTICAL OVERVIEW
            </div>
          </div>
          
          <div className="h-48 bg-[var(--surface)] border border-[var(--grid-cyan)] p-4 overflow-y-auto">
            <h2 className="text-lg text-[var(--cyan-accent)] mb-2 font-mono">TELEMETRY MATRIX</h2>
            <table className="w-full text-sm text-left font-mono">
              <thead>
                <tr className="border-b border-gray-700 text-gray-400">
                  <th className="py-1">CALLSIGN</th>
                  <th>TEMP (C)</th>
                  <th>RPM</th>
                  <th>RUL (SCORE)</th>
                  <th>ACTION</th>
                </tr>
              </thead>
              <tbody>
                {Object.values(drones).map(d => (
                  <tr key={d.droneId} className="border-b border-gray-800">
                    <td className="py-2 text-[var(--cyan-accent)]">{d.droneId}</td>
                    <td className={d.batteryTemp > 75 ? 'text-[var(--crimson-alert)] font-bold' : ''}>{d.batteryTemp.toFixed(1)}</td>
                    <td className={d.motorRpm < 2000 ? 'text-[var(--amber-alert)] font-bold' : ''}>{d.motorRpm.toFixed(0)}</td>
                    <td>{d.healthScore ? d.healthScore.toFixed(1) : '...'}</td>
                    <td>
                      <button onClick={() => overrideDrone(d.droneId)} className="text-xs border border-[var(--crimson-alert)] text-[var(--crimson-alert)] px-2 py-1 hover:bg-[var(--crimson-alert)] hover:text-white">
                        OVERRIDE
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        {/* Right Column: Video Feed & Alerts */}
        <div className="col-span-1 flex flex-col space-y-4">
          <div className="h-64 bg-black border border-[var(--cyan-accent)] relative flex items-center justify-center">
            <img 
              src="http://127.0.0.1:8000/api/ai/vision" 
              alt="YOLOv8 Observer Feed"
              className="w-full h-full object-cover"
              onError={(e) => {
                e.target.style.display = 'none';
                e.target.nextSibling.style.display = 'block';
              }}
            />
            <div className="hidden text-[var(--amber-alert)] font-mono animate-pulse">
              [NO SIGNAL DETECTED]
            </div>
            <div className="absolute top-2 right-2 text-xs text-[var(--cyan-accent)] font-mono bg-black/80 px-2 py-1">
              OBSERVER FEED [LIVE]
            </div>
          </div>
          
          <div className="flex-1 bg-[var(--surface)] border border-[var(--crimson-alert)] p-4 flex flex-col">
            <h2 className="text-lg text-[var(--crimson-alert)] mb-2 font-mono border-b border-[var(--crimson-alert)] pb-1">CRITICAL ALERTS</h2>
            <div className="flex-1 overflow-y-auto space-y-2 font-mono text-sm">
              {alerts.length === 0 && <p className="text-gray-500 italic">No active alerts.</p>}
              {alerts.map((a, i) => (
                <div key={i} className="bg-black/40 border-l-2 border-[var(--crimson-alert)] p-2">
                  <div className="text-[var(--crimson-alert)] font-bold">{a.droneId} - {a.severity}</div>
                  <div className="text-gray-300 text-xs">{a.message}</div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default App;
