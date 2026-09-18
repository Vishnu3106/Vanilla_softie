from fastapi import FastAPI, HTTPException
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
import pickle
import pandas as pd
import os
import zipfile
import cv2
import numpy as np
import time

try:
    from ultralytics import YOLO
except ImportError:
    YOLO = None

app = FastAPI(title="H.A.L.O. AI Operations Center")

MODEL_PATH = "rul_xgboost.pkl"
rul_model = None
if os.path.exists(MODEL_PATH):
    with open(MODEL_PATH, 'rb') as f:
        rul_model = pickle.load(f)

# Load YOLOv8n (will download automatically on first run if missing)
yolo_model = YOLO('yolov8n.pt') if YOLO else None

class TelemetryData(BaseModel):
    droneId: str
    timestamp: str
    batteryTemp: float
    motorRpm: float
    altitude: float
    vibrationScore: float

@app.post("/api/ai/rul")
def predict_rul(data: TelemetryData):
    if rul_model is None:
        raise HTTPException(status_code=503, detail="RUL Model not loaded.")

    df = pd.DataFrame([{
        'sensor_2': data.batteryTemp,
        'sensor_11': data.motorRpm,
        'sensor_14': data.altitude,
        'sensor_15': data.vibrationScore
    }])
    
    rul_pred = float(rul_model.predict(df)[0])
    
    # Calculate HealthScore 0-100 (assuming 130 is nominal max RUL for CMAPSS)
    health_score = max(0.0, min(100.0, (rul_pred / 130.0) * 100.0))
    
    return {
        "droneId": data.droneId, 
        "rul": rul_pred,
        "healthScore": health_score
    }

# Keeping /predict for backward compatibility until Phase 2 is finished
@app.post("/predict")
def predict_health_legacy(data: TelemetryData):
    result = predict_rul(data)
    return {"droneId": data.droneId, "healthScore": result["healthScore"]}

def generate_video_frames():
    zip_path = '../data/04_AUAIR_multimodal_uav.zip'
    if not os.path.exists(zip_path) or yolo_model is None:
        while True:
            frame = np.zeros((480, 640, 3), dtype=np.uint8)
            cv2.putText(frame, "Waiting for YOLO/Data...", (50, 240), cv2.FONT_HERSHEY_SIMPLEX, 1, (0, 0, 255), 2)
            ret, buffer = cv2.imencode('.jpg', frame)
            yield (b'--frame\r\n' b'Content-Type: image/jpeg\r\n\r\n' + buffer.tobytes() + b'\r\n')
            time.sleep(1)

    with zipfile.ZipFile(zip_path, 'r') as zf:
        # Get all image paths inside zip
        image_files = [f for f in zf.namelist() if f.startswith('04_AUAIR_multimodal_uav/images/') and f.endswith('.jpg')]
        image_files.sort()
        
        while True:
            for img_file in image_files:
                with zf.open(img_file) as f:
                    file_bytes = np.asarray(bytearray(f.read()), dtype=np.uint8)
                    frame = cv2.imdecode(file_bytes, cv2.IMREAD_COLOR)
                    
                    if frame is not None:
                        # Resize for performance and UI consistency
                        frame = cv2.resize(frame, (640, 480))
                        
                        # Run YOLO inference
                        results = yolo_model(frame, verbose=False)
                        
                        # Draw bounding boxes
                        for r in results:
                            boxes = r.boxes
                            for box in boxes:
                                x1, y1, x2, y2 = box.xyxy[0].int().tolist()
                                cls_id = int(box.cls[0])
                                class_name = yolo_model.names[cls_id]
                                
                                color = (0, 255, 0) # Green for friendlies
                                if class_name in ['person', 'car', 'truck']:
                                    color = (0, 0, 255) # Red for potential threats
                                    
                                cv2.rectangle(frame, (x1, y1), (x2, y2), color, 2)
                                cv2.putText(frame, class_name, (x1, y1 - 10), cv2.FONT_HERSHEY_SIMPLEX, 0.5, color, 2)
                        
                        # Add HUD overlays
                        cv2.putText(frame, "LIVE: AERIAL DETECTION", (10, 30), cv2.FONT_HERSHEY_SIMPLEX, 0.7, (0, 255, 255), 2)
                        
                        ret, buffer = cv2.imencode('.jpg', frame)
                        frame_bytes = buffer.tobytes()
                        yield (b'--frame\r\n'
                               b'Content-Type: image/jpeg\r\n\r\n' + frame_bytes + b'\r\n')
                time.sleep(0.1) # Simulate 10 FPS for performance

@app.get("/api/ai/vision")
def vision_stream():
    return StreamingResponse(generate_video_frames(), media_type="multipart/x-mixed-replace; boundary=frame")

@app.get("/health")
def health_check():
    return {"status": "ok", "rul_model_loaded": rul_model is not None, "yolo_model_loaded": yolo_model is not None}
