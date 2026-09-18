from fastapi import FastAPI, HTTPException, WebSocket, WebSocketDisconnect
from fastapi.responses import StreamingResponse
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import pickle
import pandas as pd
import os
import zipfile
import cv2
import numpy as np
import time
import asyncio

try:
    from ultralytics import YOLO
except ImportError:
    YOLO = None

app = FastAPI(title="H.A.L.O. AI Operations Center")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# ── RUL Model ──────────────────────────────────────────────────────────────────
MODEL_PATH = "rul_xgboost.pkl"
rul_model = None
if os.path.exists(MODEL_PATH):
    with open(MODEL_PATH, "rb") as f:
        rul_model = pickle.load(f)

# ── YOLOv8 ────────────────────────────────────────────────────────────────────
yolo_model = YOLO("yolov8n.pt") if YOLO else None

# ── Data paths ────────────────────────────────────────────────────────────────
DATA_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "data"))
AUAIR_ZIP   = os.path.join(DATA_DIR, "04_AUAIR_multimodal_uav.zip")
VISDRONE_ZIP = os.path.join(DATA_DIR, "05_VisDrone_detection_tracking.zip")


# ── Pydantic schema ───────────────────────────────────────────────────────────
class TelemetryData(BaseModel):
    droneId: str
    timestamp: str = ""
    batteryTemp: float
    motorRpm: float
    altitude: float
    vibrationScore: float
    latitude: float = 0.0
    longitude: float = 0.0
    status: str = "ACTIVE"


# ── RUL Prediction ────────────────────────────────────────────────────────────
@app.post("/api/ai/rul")
def predict_rul(data: TelemetryData):
    if rul_model is None:
        raise HTTPException(status_code=503, detail="RUL Model not loaded.")
    df = pd.DataFrame([{
        "sensor_2":  data.batteryTemp,
        "sensor_11": data.motorRpm,
        "sensor_14": data.altitude,
        "sensor_15": data.vibrationScore,
    }])
    rul_pred = float(rul_model.predict(df)[0])
    health_score = max(0.0, min(100.0, (rul_pred / 130.0) * 100.0))
    return {"droneId": data.droneId, "rul": rul_pred, "healthScore": health_score}


@app.post("/predict")
def predict_health_legacy(data: TelemetryData):
    result = predict_rul(data)
    return {"droneId": data.droneId, "healthScore": result["healthScore"]}


# ── Frame annotation helper ───────────────────────────────────────────────────
def annotate_frame(frame: np.ndarray) -> np.ndarray:
    frame = cv2.resize(frame, (640, 480))
    if yolo_model is not None:
        results = yolo_model(frame, verbose=False)
        for r in results:
            for box in r.boxes:
                x1, y1, x2, y2 = box.xyxy[0].int().tolist()
                cls_id = int(box.cls[0])
                conf   = float(box.conf[0])
                label  = yolo_model.names[cls_id]
                color  = (0, 0, 255) if label in ["person", "car", "truck", "bus"] else (0, 255, 0)
                cv2.rectangle(frame, (x1, y1), (x2, y2), color, 2)
                cv2.putText(frame, f"{label} {conf:.2f}", (x1, max(y1 - 8, 12)),
                            cv2.FONT_HERSHEY_SIMPLEX, 0.45, color, 2)
    # HUD overlay
    cv2.putText(frame, "HALO // LIVE AERIAL DETECTION", (8, 24),
                cv2.FONT_HERSHEY_SIMPLEX, 0.6, (0, 255, 255), 2)
    ts = time.strftime("%H:%M:%S UTC")
    cv2.putText(frame, ts, (480, 24), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (180, 180, 180), 1)
    # Corner crosshair marks
    for (cx, cy) in [(16, 40), (624, 40), (16, 460), (624, 460)]:
        cv2.drawMarker(frame, (cx, cy), (0, 255, 255), cv2.MARKER_CROSS, 12, 1)
    return frame


def _iter_zip_images(zip_path: str, prefix: str, offset: int = 0):
    """Yield decoded images from a zip archive in sorted order, looping."""
    with zipfile.ZipFile(zip_path, "r") as zf:
        imgs = sorted(f for f in zf.namelist() if f.startswith(prefix) and f.lower().endswith((".jpg", ".jpeg", ".png")))
        if not imgs:
            return
        while True:
            for i in range(offset, len(imgs)):
                with zf.open(imgs[i]) as f:
                    buf = np.asarray(bytearray(f.read()), dtype=np.uint8)
                    frame = cv2.imdecode(buf, cv2.IMREAD_COLOR)
                    if frame is not None:
                        yield frame
            offset = 0


def _synthetic_frames(drone_id: str = ""):
    """Fallback: generate synthetic tactical frames when no dataset is present."""
    frame_idx = 0
    while True:
        frame = np.zeros((480, 640, 3), dtype=np.uint8)
        # Animated scan bar
        bar_y = int((time.time() * 60) % 480)
        cv2.line(frame, (0, bar_y), (640, bar_y), (0, 255, 255), 1)
        # Grid
        for x in range(0, 640, 80):
            cv2.line(frame, (x, 0), (x, 480), (0, 40, 40), 1)
        for y in range(0, 480, 60):
            cv2.line(frame, (0, y), (640, y), (0, 40, 40), 1)
        cv2.putText(frame, f"NO SENSOR FEED — {drone_id} SYNTHETIC MODE", (60, 240),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.65, (0, 200, 200), 2)
        cv2.putText(frame, f"FRAME {frame_idx:06d}", (230, 270),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.45, (120, 120, 120), 1)
        frame_idx += 1
        yield frame


def _get_frame_source(drone_id: str = ""):
    # Use deterministic hash to assign a consistent footage sequence to a drone
    import hashlib
    h = int(hashlib.sha256(drone_id.encode()).hexdigest(), 16) if drone_id else 0
    
    if os.path.exists(AUAIR_ZIP):
        # AUAIR has multiple distinct video sequences
        prefixes = [
            '04_AUAIR_multimodal_uav/images/frame_20190829091111', 
            '04_AUAIR_multimodal_uav/images/frame_20190906150731', 
            '04_AUAIR_multimodal_uav/images/frame_20190905091750', 
            '04_AUAIR_multimodal_uav/images/frame_20190905103112', 
            '04_AUAIR_multimodal_uav/images/frame_20190829091316', 
            '04_AUAIR_multimodal_uav/images/frame_20190905112522'
        ]
        prefix = prefixes[h % len(prefixes)]
        return _iter_zip_images(AUAIR_ZIP, prefix, 0)
        
    if os.path.exists(VISDRONE_ZIP):
        return _iter_zip_images(VISDRONE_ZIP, "VisDrone2019-DET-train/images/", h % 500)
        
    return _synthetic_frames(drone_id)


# ── MJPEG HTTP stream (legacy) ─────────────────────────────────────────────────
def generate_video_frames(drone_id: str = ""):
    for raw_frame in _get_frame_source(drone_id):
        frame = annotate_frame(raw_frame)
        ret, buffer = cv2.imencode(".jpg", frame, [cv2.IMWRITE_JPEG_QUALITY, 75])
        if ret:
            yield b"--frame\r\nContent-Type: image/jpeg\r\n\r\n" + buffer.tobytes() + b"\r\n"
        time.sleep(0.12)  # ~8 FPS


@app.get("/api/ai/vision")
def vision_stream(droneId: str = ""):
    return StreamingResponse(generate_video_frames(droneId), media_type="multipart/x-mixed-replace; boundary=frame")


# ── WebSocket binary vision stream ────────────────────────────────────────────
@app.websocket("/ws/vision")
async def websocket_vision(websocket: WebSocket):
    await websocket.accept()
    drone_id = websocket.query_params.get("droneId", "")
    loop = asyncio.get_event_loop()
    try:
        frame_gen = _get_frame_source(drone_id)
        while True:
            raw_frame = await loop.run_in_executor(None, next, frame_gen)
            annotated  = await loop.run_in_executor(None, annotate_frame, raw_frame)
            ret, buf   = cv2.imencode(".jpg", annotated, [cv2.IMWRITE_JPEG_QUALITY, 72])
            if ret:
                await websocket.send_bytes(buf.tobytes())
            await asyncio.sleep(0.12)  # ~8 FPS
    except (WebSocketDisconnect, Exception):
        pass


@app.get("/health")
def health_check():
    return {
        "status": "ok",
        "rul_model_loaded": rul_model is not None,
        "yolo_model_loaded": yolo_model is not None,
        "auair_dataset": os.path.exists(AUAIR_ZIP),
        "visdrone_dataset": os.path.exists(VISDRONE_ZIP),
    }