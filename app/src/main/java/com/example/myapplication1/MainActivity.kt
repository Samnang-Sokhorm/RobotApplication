package com.example.myapplication1

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException
import java.util.ArrayDeque

class MainActivity : AppCompatActivity() {

    private lateinit var gameView: GameView
    private val client = OkHttpClient()
    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 20L // 50 times/sec
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var buttonRestart: Button

    private val positionBuffer = ArrayDeque<EncoderPosition>()
    private var joystickX = 0f
    private var joystickY = 0f
    private var joystickZ = 0f


    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            // Capture right joystick movements (Z-axis) for rotation
            joystickZ = event.getAxisValue(MotionEvent.AXIS_Z)  // Right joystick (Z-axis for rotation)
            joystickX = event.getAxisValue(MotionEvent.AXIS_X)  // Right joystick (X-axis for movement)
            joystickY = event.getAxisValue(MotionEvent.AXIS_Y)  // Right joystick (Y-axis for movement)

            // Move the unit based on joystick input


            // Optionally update the UI or send data to the server
            sendJoystickDataToServer(joystickX, joystickY, joystickZ)
        }
        return super.onGenericMotionEvent(event)
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        gameView = GameView(this)
        setContentView(gameView)

        startDataFetchLoop()
        startRenderLoop()

    }
    private fun sendJoystickDataToServer(joystickX: Float, joystickY: Float, joystickZ: Float) {
        val url = "http://192.168.100.3:5000/update_position"  // Update this URL to your server's endpoint

        val json = JSONObject().apply {
            put("x",joystickX)
            put("y", -(joystickY))
            put("z", joystickZ)
        }

        val requestBody = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json.toString())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Handle failure, maybe notify the user
            }

            override fun onResponse(call: Call, response: Response) {
                // Handle success, you can log the response if needed
            }
        })
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
    }

    private fun startDataFetchLoop() {
        coroutineScope.launch {
            while (isActive) {
                fetchDataFromServer()
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

    private fun fetchDataFromServer() {
        val request = Request.Builder()
            .url("http://192.168.100.3:6000/get_arduino_data")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("MainActivity", "Failed to fetch data", e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    if (response.isSuccessful) {
                        response.body?.string()?.let {
                            val json = JSONObject(it)
                            val x1 = json.optDouble("X1", 0.0).toFloat()
                            val y1 = json.optDouble("Y1", 0.0).toFloat()
                            val z = json.optDouble("Z", 0.0).toFloat()

                            Log.d("ServerData", "Received from server -> X1=$x1, Y1=$y1, Z=$z")

                            val newPos = EncoderPosition(x1, y1, z)
                            val smoothed = smoothPosition(newPos)

                            handler.post {
                                gameView.updateOdometry(smoothed.X1, smoothed.Y1, smoothed.Z)
                            }
                        }
                    } else {
                        Log.e("MainActivity", "Server error: ${response.code}")
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Parsing or network error", e)
                }
            }
        })
    }

    private fun smoothPosition(newPos: EncoderPosition): EncoderPosition {
        if (positionBuffer.size >= 5) positionBuffer.removeFirst()
        positionBuffer.addLast(newPos)

        val avgX = positionBuffer.map { it.X1 }.average().toFloat()
        val avgY = positionBuffer.map { it.Y1 }.average().toFloat()
        val avgZ = positionBuffer.map { it.Z }.average().toFloat()

        Log.d("Smoothing", "Averaged -> X=$avgX, Y=$avgY, Z=$avgZ")

        return EncoderPosition(avgX, avgY, avgZ)
    }
}

data class EncoderPosition(val X1: Float, val Y1: Float, val Z: Float)
