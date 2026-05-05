package com.example.myapplication1

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication1.AppConfig.SERVER_BASE_URL
import kotlinx.coroutines.*
import org.json.JSONObject
import io.socket.client.IO
import io.socket.client.Socket

private var dpadUpWasPressed = false
private var dpadDownWasPressed = false
private var dpadRightWasPressed = false
private var dpadLeftWasPressed = false

class MainActivity : AppCompatActivity() {

    private lateinit var gameView: GameView
    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 20L // 50 times/sec
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var socket: Socket

    private var joystickX = 0f
    private var joystickY = 0f
    private var joystickZ = 0f
    private var lbButton = 0
    private var rbButton = 0
    private var ltButton = 0
    private var rtButton = 0
    private var m1Button = 0
    private var m2Button = 0
    private var aButton = 0
    private var xButton = 0
    private var yButton = 0

    private var f1Button = 0
    private var b1Button = 0
    private var r1Button = 0
    private var l1Button = 0
    private var bButton = 0



//    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
//        if (event != null) {
//            // Capture right joystick movements (Z-axis) for rotation
//            joystickZ =
//                event.getAxisValue(MotionEvent.AXIS_Z)  // Right joystick (Z-axis for rotation)
//            joystickX =
//                event.getAxisValue(MotionEvent.AXIS_X)  // Right joystick (X-axis for movement)
//            joystickY =
//                event.getAxisValue(MotionEvent.AXIS_Y)  // Right joystick (Y-axis for movement)
//            updateAndSendControllerState()
//            return true
//        }
//        return super.onGenericMotionEvent(event)
//    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        if (event != null) {

            joystickZ = event.getAxisValue(MotionEvent.AXIS_Z)
            joystickX = event.getAxisValue(MotionEvent.AXIS_X)
            joystickY = event.getAxisValue(MotionEvent.AXIS_Y)

            // D-pad / HAT axis
            val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
            val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)

            // UP: hatY = -1
            if (hatY < -0.5f) {
                if (!dpadUpWasPressed) {
                    dpadUpWasPressed = true
                    f1Button = 1 - f1Button
                    Log.e("DPAD_HAT_DEBUG", "F1 toggle=$f1Button")
                    updateAndSendControllerState()
                }
            } else {
                dpadUpWasPressed = false
            }

            // DOWN: hatY = 1
            if (hatY > 0.5f) {
                if (!dpadDownWasPressed) {
                    dpadDownWasPressed = true
                    b1Button = 1 - b1Button
                    Log.e("DPAD_HAT_DEBUG", "B1 toggle=$b1Button")
                    updateAndSendControllerState()
                }
            } else {
                dpadDownWasPressed = false
            }

            // RIGHT: hatX = 1
            if (hatX > 0.5f) {
                if (!dpadRightWasPressed) {
                    dpadRightWasPressed = true
                    r1Button = 1 - r1Button
                    Log.e("DPAD_HAT_DEBUG", "R1 toggle=$r1Button")
                    updateAndSendControllerState()
                }
            } else {
                dpadRightWasPressed = false
            }

            // LEFT: hatX = -1
            if (hatX < -0.5f) {
                if (!dpadLeftWasPressed) {
                    dpadLeftWasPressed = true
                    l1Button = 1 - l1Button
                    Log.e("DPAD_HAT_DEBUG", "L1 toggle=$l1Button")
                    updateAndSendControllerState()
                }
            } else {
                dpadLeftWasPressed = false
            }

            // Send joystick updates too
            updateAndSendControllerState()
            return true
        }

        return super.onGenericMotionEvent(event)
    }

    private fun setupSocket() {
        try {
            socket = IO.socket(SERVER_BASE_URL)

            socket.on(Socket.EVENT_CONNECT) {
                Log.d("SocketIO", "Connected to server")
            }

            socket.on(Socket.EVENT_DISCONNECT) {
                Log.d("SocketIO", "Disconnected from server")
            }

            socket.on("server_ack") { args ->
                if (args.isNotEmpty()) {
                    Log.d("SocketIO", "ACK: ${args[0]}")
                }
            }

            socket.on("teensy_data") { args ->
                if (args.isNotEmpty()) {
                    Log.d("TeensyData", args[0].toString())
                }
            }

            socket.connect()

        } catch (e: Exception) {
            Log.e("SocketIO", "Socket setup error", e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        gameView = GameView(this)
        setContentView(gameView)
        setupSocket()
        startDataFetchLoop()
        startRenderLoop()

    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        var handled = true
        when (event.keyCode) {
            Log.e("KEY_LOGGER", "keyCode=${event.keyCode}, " +
                    "name=${KeyEvent.keyCodeToString(event.keyCode)}, " +
                    "action=${event.action}, repeat=${event.repeatCount}"),
            KeyEvent.KEYCODE_BUTTON_L1 -> {
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    lbButton = if (lbButton == 0) 1 else 0
                    Log.d("LB Button", "LB Button state: $lbButton")
                    updateAndSendControllerState()
                }
                return true
            }
            KeyEvent.KEYCODE_BUTTON_THUMBL -> {
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    m1Button = if (m1Button == 0) 1 else 0
                    Log.d("M1 Button", "M1 Button state: $m1Button")
                    updateAndSendControllerState()
                }
                return true
            }

            KeyEvent.KEYCODE_BUTTON_THUMBR -> {
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    m2Button = if (m2Button == 0) 1 else 0
                    Log.d("M2 Button", "M2 Button state: $m2Button")
                    updateAndSendControllerState()
                }
                return true
            }
            KeyEvent.KEYCODE_BUTTON_R1 -> {
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    rbButton = if (rbButton == 0) 1 else 0
                    Log.d("RB Button", "RB Button state: $rbButton")
                    updateAndSendControllerState()
                }
                return true
            }

            KeyEvent.KEYCODE_BUTTON_L2 -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    ltButton = 1
                    Log.d("LT Button", "LT Button state: $ltButton")
                    updateAndSendControllerState()
                }
                return true
                if (event.action == KeyEvent.ACTION_UP) {
                    ltButton = 0
                    Log.d("LT Button", "LT Button state: $ltButton")
                    updateAndSendControllerState()
                }
                return true
//                KeyEvent.KEYCODE_BUTTON_L2 -> {
//                    if (event.action == android.view.KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
//                    ltButton = if (ltButton == 0) 1 else 0
//                    Log.d("LT Button", "LT Button state: $ltButton")
//                    gameView.updateControllerState(
//                        joystickX,
//                        joystickY,
//                        joystickZ,
//                        lbButton,
//                        rbButton,
//                        ltButton,
//                        rtButton,
//                        m1Button,
//                        m2Button
//                    )
//                    sendJoystickDataToServer(joystickX, joystickY, joystickZ, lbButton, rbButton, ltButton, rtButton, m1Button, m2Button)
//                    return true
//                        }
            }

            KeyEvent.KEYCODE_BUTTON_R2 -> {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    rtButton = 1
                    Log.d("RT Button", "RT Button state: $rtButton")
                    updateAndSendControllerState()
                }
                return true
                if (event.action == KeyEvent.ACTION_UP) {
                    rtButton = 0
                    Log.d("RT Button", "RT Button state: $rtButton")
                    updateAndSendControllerState()
                }
                    return true
            }


            KeyEvent.KEYCODE_BUTTON_X -> {
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    xButton = if (xButton == 0) 1 else 0
                    Log.d("ButtonHold", "X Button state: $xButton")
                    updateAndSendControllerState()
                }
                return true
            }

            KeyEvent.KEYCODE_BUTTON_Y -> {
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    yButton = if (yButton == 0) 1 else 0
                    Log.d("ButtonHold", "Y Button state: $yButton")
                    updateAndSendControllerState()
                }
                return true
            }

            KeyEvent.KEYCODE_BUTTON_A -> {
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    aButton = if (aButton == 0) 1 else 0
                    Log.d("ButtonHold", "A Button state: $aButton")
                    updateAndSendControllerState()
                }
                return true
            }
            KeyEvent.KEYCODE_BUTTON_B -> {
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    bButton = if (bButton == 0) 1 else 0
                    Log.d("B Button", "B state: $bButton")
                    updateAndSendControllerState()
                }
                return true  // ← this line BLOCKS the back navigation
            }

            KeyEvent.KEYCODE_DPAD_UP -> {
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    f1Button = 1 - f1Button
                    Log.e("DPAD_DEBUG", "F1=$f1Button")
                    updateAndSendControllerState()
                }
                return true
            }

            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    b1Button = 1 - b1Button
                    Log.e("DPAD_DEBUG", "B1=$b1Button")
                    updateAndSendControllerState()
                }
                return true
            }

            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    r1Button = 1 - r1Button
                    Log.e("DPAD_DEBUG", "R1=$r1Button")
                    updateAndSendControllerState()
                }
                return true
            }

            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                    l1Button = 1 - l1Button
                    Log.e("DPAD_DEBUG", "L1=$l1Button")
                    updateAndSendControllerState()
                }
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }
    private fun sendJoystickDataToServer(
        joystickX: Float,
        joystickY: Float,
        joystickZ: Float,
        lbButton: Int,
        rbButton: Int,
        ltButton: Int,
        rtButton: Int,
        m1Button: Int,
        m2Button: Int,
        xButton: Int,
        yButton: Int,
        aButton: Int,
        bButton: Int,
        f1Button: Int,
        b1Button: Int,
        r1Button: Int,
        l1Button: Int
    )  {
        if (!::socket.isInitialized || !socket.connected()) {
            Log.e("SocketIO", "Socket not connected")
            return
        }

        val json = JSONObject().apply {
            put("x", joystickX)
            put("y", -joystickY)
            put("z", joystickZ)
            put("LB", lbButton)
            put("RB", rbButton)
            put("LT", ltButton)
            put("RT", rtButton)
            put("M1", m1Button)
            put("M2", m2Button)
            put("X1", xButton)
            put("Y1", yButton)
            put("A1", aButton)
            put("Bb", bButton)
            put("F1", f1Button)
            put("B1", b1Button)
            put("R1", r1Button)
            put("L1", l1Button)


        }

        Log.d("SocketIO", "Emit controller_data: $json")

        socket.emit("controller_data", json)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Log.d("Orientation", "Landscape mode")
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            Log.d("Orientation", "Portrait mode")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()

        if (::socket.isInitialized) {
            socket.disconnect()
            socket.off()
        }
    }

//    override fun onDestroy() {
//        super.onDestroy()
//        coroutineScope.cancel()
//    }

    private fun updateAndSendControllerState() {
        gameView.updateControllerState(
            joystickX,
            joystickY,
            joystickZ,
            lbButton,
            rbButton,
            ltButton,
            rtButton,
            m1Button,
            m2Button,
            xButton,
            yButton,
            aButton,
            bButton,
            this.f1Button,
            this.b1Button,
            this.r1Button,
            this.l1Button
        )

        sendJoystickDataToServer(
            joystickX,
            joystickY,
            joystickZ,
            lbButton,
            rbButton,
            ltButton,
            rtButton,
            m1Button,
            m2Button,
            xButton,
            yButton,
            aButton,
            bButton,
            this.f1Button,
            this.b1Button,
            this.r1Button,
            this.l1Button
        )
    }
    private fun startDataFetchLoop() {
        coroutineScope.launch {
            while (isActive) {
//                fetchDataFromServer()
                delay(updateInterval)
            }
        }
    }

    private fun startRenderLoop() {
        handler.post(object : Runnable {
            override fun run() {
                gameView.postInvalidate()
                handler.postDelayed(this, 16L) // ~60 FPS
            }
        })
    }
}

