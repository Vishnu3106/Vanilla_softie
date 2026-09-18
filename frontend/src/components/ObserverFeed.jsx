import React from 'react';

export default function ObserverFeed() {
    return (
        <div className="card observer-card">
            <h2>Observer Feed (AERIAL DETECTION)</h2>
            <div className="video-container">
                {/* Simulated multimodal visual feed */}
                <div className="bounding-box drone-box"></div>
                <div className="bounding-box object-box"></div>
                <div className="overlay-text">LIVE: AERIAL DETECTION ACTIVE</div>
            </div>
        </div>
    );
}
