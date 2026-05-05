package com.example.myapplication1

object AppConfig {
    // Change only this IP/URL when your Flask server IP changes.
    const val SERVER_BASE_URL = "http://10.240.0.138:5000"

    const val UPDATE_POSITION_URL = "$SERVER_BASE_URL/update_position"
}
