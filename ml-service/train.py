import pandas as pd
import xgboost as xgb
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_squared_error
import pickle
import os
import zipfile

def train():
    zip_path = '../data/01_NASA_CMAPSS_predictive_maintenance.zip'
    if not os.path.exists(zip_path):
        print(f"Data not found at {zip_path}.")
        return

    # CMAPSS columns
    columns = ['unit_number', 'time_in_cycles', 'setting_1', 'setting_2', 'setting_3'] + [f'sensor_{i}' for i in range(1, 22)]
    
    with zipfile.ZipFile(zip_path, 'r') as zf:
        with zf.open('01_NASA_CMAPSS_predictive_maintenance/CMAPSSData/train_FD001.txt') as f:
            df = pd.read_csv(f, sep=r'\s+', header=None, names=columns)
    
    # Calculate RUL (Remaining Useful Life)
    rul = pd.DataFrame(df.groupby('unit_number')['time_in_cycles'].max()).reset_index()
    rul.columns = ['unit_number', 'max']
    df = df.merge(rul, on=['unit_number'], how='left')
    df['RUL'] = df['max'] - df['time_in_cycles']
    df.drop('max', axis=1, inplace=True)
    
    # To maintain compatibility with our hackathon backend without rewriting Java POJOs, 
    # we map 4 CMAPSS sensors to our existing drone telemetry schema:
    # batteryTemp -> sensor_2 (temperature)
    # motorRpm -> sensor_11 (rpm proxy)
    # altitude -> sensor_14 (proxy)
    # vibrationScore -> sensor_15 (proxy)
    
    X = df[['sensor_2', 'sensor_11', 'sensor_14', 'sensor_15']]
    y = df['RUL']
    
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

    model = xgb.XGBRegressor(objective='reg:squarederror', n_estimators=100, learning_rate=0.1)
    model.fit(X_train, y_train)

    preds = model.predict(X_test)
    rmse = mean_squared_error(y_test, preds) ** 0.5
    print(f"CMAPSS Model trained. RMSE: {rmse:.2f}")

    # Save model
    with open('rul_xgboost.pkl', 'wb') as f:
        pickle.dump(model, f)
    print("Model saved to rul_xgboost.pkl")

if __name__ == '__main__':
    train()
