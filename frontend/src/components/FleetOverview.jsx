import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { Activity, AlertTriangle, CheckCircle2 } from 'lucide-react';

export default function FleetOverview() {
    const [health, setHealth] = useState({});
    
    // Tracking the OpenSky drone fleet
    const drones = ['IGO1477', 'AIC1ZH', 'THA340'];

    useEffect(() => {
        const fetchHealth = async () => {
            const newHealth = { ...health };
            for (let drone of drones) {
                try {
                    const res = await axios.get(`http://localhost:8081/api/health/${drone}`);
                    newHealth[drone] = res.data;
                } catch (err) {
                    console.error("Error fetching health for", drone);
                }
            }
            setHealth(newHealth);
        };
        fetchHealth();
        const interval = setInterval(fetchHealth, 2000);
        return () => clearInterval(interval);
    }, []);

    return (
        <div className="card">
            <h2>Fleet Overview</h2>
            <div className="grid">
                {drones.map(drone => {
                    const score = health[drone] ?? 100;
                    const isCritical = score < 50;
                    return (
                        <div key={drone} className={`stat-box ${isCritical ? 'critical' : 'healthy'}`}>
                            <div className="stat-header">
                                <h3>{drone}</h3>
                                {isCritical ? <AlertTriangle size={20} style={{ color: 'var(--neon-red)' }}/> : <CheckCircle2 size={20} style={{ color: 'var(--neon-green)' }}/>}
                            </div>
                            <div className="stat-score">{score.toFixed(1)} / 100</div>
                            <div className="stat-label">Health Score</div>
                        </div>
                    );
                })}
            </div>
        </div>
    );
}
