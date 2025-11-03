from flask import Flask, request, jsonify
import time
import serial
import json

app = Flask(__name__)

# Try opening serial connection to Arduino
try:
    ser = serial.Serial('/dev/ttyACM1', 115200, timeout=0.1)
    time.sleep(2)
except serial.SerialException as e:
    print(f"Error opening serial port: {e}")
    ser = None

# ---- Global state ----
position_data = {"x": 0.0, "y": 0.0, "z": 0.0}
last_update_time = time.time()
data_available = False
last_send = 0


# ---- Helper function ----
def send_position_to_arduino():
    global last_send
    if time.time() - last_send < 0.05:  # Limit: 20 sends/sec
        return
    last_send = time.time()

    if ser and ser.is_open:
        try:
            position_str = json.dumps(position_data) + "\n"
            ser.write(position_str.encode())

            # Print only when movement is noticeable
            if abs(position_data["x"]) > 0.01 or abs(position_data["y"]) > 0.01:
                print(f"Sent to Arduino: {position_str.strip()}")
        except serial.SerialException as e:
            print(f"⚠️ Serial write failed: {e}")
    else:
        print("⚠️ Serial port not available. Data not sent.")


# ---- Routes ----
@app.route('/')
def home():
    return 'Welcome to the robot control server!'


@app.route('/update_position', methods=['POST'])
def update_position():
    global position_data, last_update_time, data_available

    data = request.get_json(force=True, silent=True)
    if not data:
        return jsonify({"error": "Invalid JSON"}), 400

    new_x = data.get('x')
    new_y = data.get('y')
    new_z = data.get('z')

    if new_x is None or new_y is None or new_z is None:
        return jsonify({"error": "Missing x, y, or z"}), 400

    position_data = {"x": new_x, "y": new_y, "z": new_z}
    last_update_time = time.time()
    data_available = True

    print(f"Updated position: X={new_x}, Y={new_y}, Z={new_z}")
    send_position_to_arduino()

    return jsonify({"status": "success", **position_data})


@app.route('/update_joystick', methods=['POST'])
def update_joystick():
    global position_data

    data = request.get_json(force=True, silent=True)
    if not data:
        return jsonify({"error": "Invalid JSON"}), 400

    jx = data.get('joystick_x')
    jy = data.get('joystick_y')
    btn = data.get('button_pressed')

    if jx is None or jy is None or btn is None:
        return jsonify({"error": "Missing joystick data"}), 400

    position_data['x'] += jx
    position_data['y'] += jy
    position_data['z'] = 0

    print(f"Joystick: X={jx}, Y={jy}, Button={btn}")
    send_position_to_arduino()

    return jsonify({
        "status": "success",
        "new_position": position_data
    })


@app.route('/get_position', methods=['GET'])
def get_position():
    global position_data, data_available, last_update_time

    # Reset if no update for a while
    if time.time() - last_update_time > 2.0:
        position_data = {"x": 0.0, "y": 0.0, "z": 0.0}
        data_available = False

    return jsonify({
        "data_available": data_available,
        **position_data
    })


if __name__ == "__main__":
    app.run(host='0.0.0.0', port=5000, debug=False, threaded=True)
