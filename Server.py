from flask import Flask, request, jsonify
import time
import serial
import json
x=0
y=0
z=0
app = Flask(__name__)

# Try opening serial connection to Arduino
try:
    ser = serial.Serial('/dev/ttyACM0', 115200, timeout=0.1)  # Adjust to your port
    time.sleep(2)  # Give time for Arduino to reset
except serial.SerialException as e:
    print(f"Error opening serial port: {e}");
    ser = None  # Handle case where serial port isn't available

# Store position data globally
position_data = {"x": 0.0, "y": 0.0, "z":  0.0}
last_update_time = time.time()
data_available = False  # Flag to check if joystick/button data is available

# Function to send position data to Arduino
def send_position_to_arduino():
    if ser and ser.is_open:
        position_str = json.dumps(position_data) + "\n"
        ser.write(position_str.encode())  # Send data to Arduino
        print(f"Sent to Arduino: {position_str.strip()}")
    else:
        print("Serial port not available. Data not sent.")

# Root URL
@app.route('/')
def home():
    return 'Welcome to the server!'

# Update position via POST
@app.route('/update_position', methods=['POST'])
def update_position():
    global position_data, last_update_time, data_available

    data = request.get_json()
    if not data:
        return jsonify({"error": "Invalid JSON"}), 400

    x, y, z = data.get('x'), data.get('y'), data.get('z')
    if x is None or y is None or z is None:
        return jsonify({"error": "Missing x, y, or z values"}), 400

    position_data = {"x": x, "y": y, "z": z}
    last_update_time = time.time()
    data_available = True

    print(f"Updated position: X={x}, Y={y}, Z={z}")
    send_position_to_arduino()

    return jsonify({"status": "success", "x": x, "y": y, "z": z})

# Update joystick data via POST
@app.route('/update_joystick', methods=['POST'])
def update_joystick():
    global position_data

    data = request.get_json()
    if not data:
        return jsonify({"error": "Invalid JSON"}), 400

    joystick_x, joystick_y, button_pressed = data.get('joystick_x'), data.get('joystick_y'), data.get('button_pressed')
    if joystick_x is None or joystick_y is None or button_pressed is None:
        return jsonify({"error": "Missing joystick_x, joystick_y, or button_pressed values"}), 400

    position_data['x'] += joystick_x
    position_data['y'] += joystick_y
    position_data['z'] = 0  # Example: Z stays at 0 for simplicity

    print(f"Joystick moved: X={joystick_x}, Y={joystick_y}, Button={button_pressed}")
    send_position_to_arduino()

    return jsonify({
        "status": "success",
        "joystick_x": joystick_x,
        "joystick_y": joystick_y,
        "button_pressed": button_pressed,
        "new_position": position_data
    })

# Get latest position via GET
@app.route('/get_position', methods=['GET'])
def get_position():
    global position_data, data_available

    # Reset position if no update in last 0.3 seconds
    if time.time() - last_update_time > 0.3:
        position_data = {"x": 0.0, "y": 0.0, "z": 0.0}
        data_available = False

    return jsonify({
        "data_available": data_available,
        "x": position_data["x"],
        "y": position_data["y"],
        "z": position_data["z"]
    })

if __name__ == "__main__":
    app.run(host='0.0.0.0', port=5000, debug=True)
