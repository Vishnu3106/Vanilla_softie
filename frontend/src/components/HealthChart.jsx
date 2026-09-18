import React, { useEffect, useState } from 'react';
import { Line } from 'react-chartjs-2';
import {
  Chart as ChartJS, CategoryScale, LinearScale,
  PointElement, LineElement, Title, Tooltip, Legend
} from 'chart.js';
import axios from 'axios';

ChartJS.register(
  CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend
);

export default function HealthChart() {
    const [chartData, setChartData] = useState({
        labels: [],
        datasets: [{
            label: 'AIC1ZH Health Score',
            data: [],
            borderColor: 'rgb(75, 192, 192)',
            tension: 0.1
        }]
    });

    useEffect(() => {
        const eventSource = new EventSource('http://localhost:8081/api/health/stream/AIC1ZH');

        eventSource.onmessage = (event) => {
            const score = JSON.parse(event.data);
            
            setChartData(prev => {
                const newLabels = [...prev.labels, new Date().toLocaleTimeString()].slice(-20);
                const newData = [...prev.datasets[0].data, score].slice(-20);
                return {
                    ...prev,
                    labels: newLabels,
                    datasets: [{ ...prev.datasets[0], data: newData }]
                };
            });
        };

        eventSource.onerror = (error) => {
            console.error("SSE Health Stream Error", error);
        };

        return () => eventSource.close();
    }, []);

    return (
        <div className="card">
            <h2>Real-time Health Trends</h2>
            <Line 
                data={chartData} 
                options={{ 
                    animation: false,
                    responsive: true,
                    plugins: {
                        legend: { labels: { color: '#94a3b8' } }
                    },
                    scales: {
                        x: {
                            grid: { color: 'rgba(255, 255, 255, 0.05)' },
                            ticks: { color: '#94a3b8' }
                        },
                        y: {
                            grid: { color: 'rgba(255, 255, 255, 0.05)' },
                            ticks: { color: '#94a3b8' },
                            min: 0,
                            max: 100
                        }
                    },
                    elements: {
                        line: {
                            borderColor: '#00f0ff',
                            borderWidth: 2,
                            shadowColor: '#00f0ff',
                            shadowBlur: 10
                        },
                        point: {
                            backgroundColor: '#00f0ff',
                            borderColor: '#fff',
                            borderWidth: 1,
                            radius: 3
                        }
                    }
                }} 
            />
        </div>
    );
}
