import React, { useEffect, useState } from 'react';
import { AlertOctagon } from 'lucide-react';

export default function AlertFeed() {
    const [alerts, setAlerts] = useState([]);

    useEffect(() => {
        const eventSource = new EventSource('http://localhost:8081/api/alerts/stream');

        eventSource.onmessage = (event) => {
            const newAlert = JSON.parse(event.data);
            
            setAlerts(prev => {
                const key = `${newAlert.droneId}-${newAlert.message}`;
                let exists = false;
                
                let updated = prev.map(a => {
                    if (`${a.droneId}-${a.message}` === key) {
                        exists = true;
                        return { ...a, count: (a.count || 1) + 1, timestamp: newAlert.timestamp };
                    }
                    return a;
                });
                
                if (!exists) {
                    updated.push({ ...newAlert, count: 1 });
                }
                
                // Sort by latest timestamp descending
                return updated.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
            });
        };

        eventSource.onerror = (error) => {
            console.error("SSE Alert Stream Error", error);
        };

        return () => eventSource.close();
    }, []);

    return (
        <div className="card">
            <h2>Critical Alert Feed</h2>
            <div className="alert-list">
                {alerts.length === 0 ? <p>No critical alerts.</p> : null}
                {alerts.map((alert, i) => (
                    <div key={i} className={`alert-item ${alert.severity.toLowerCase()}`}>
                        <AlertOctagon size={16} />
                        <div style={{ flex: 1 }}>
                            <strong>{alert.droneId}</strong>: {alert.message}
                            <div className="timestamp">Last seen: {alert.timestamp}</div>
                        </div>
                        {alert.count > 1 && (
                            <div className="alert-badge">
                                x{alert.count}
                            </div>
                        )}
                    </div>
                ))}
            </div>
        </div>
    );
}
