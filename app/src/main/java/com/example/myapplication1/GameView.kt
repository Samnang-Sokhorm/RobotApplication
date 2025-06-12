package com.example.myapplication1

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class GameView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val metersToPixels = 100f // 1 meter = 100 pixels
    private val mapLengthPixels = 14.80f * metersToPixels
    private val mapWidthPixels = 6f * metersToPixels

    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private var scaleX = 1f
    private var scaleY = 1f

    private val robotHalfWidth = 30f
    private val robotHalfHeight = 20f

    private var posX = mapLengthPixels / 2f
    private var posY = mapWidthPixels / 2f
    private var theta = 0f // in degrees

    private var Xx: Float = 0.0f
    private var Yy: Float = 0.0f
    private var Zz: Float = 0.0f


    private var offsetX = 0f
    private var offsetY = 0f

    private val paint = Paint().apply { color = Color.RED }

    private val mapPaint = Paint().apply {
        color = Color.LTGRAY
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        scaleX = width / mapLengthPixels
        scaleY = height / mapWidthPixels

        offsetX = (width - mapLengthPixels) / 2f
        offsetY = (height - mapWidthPixels) / 2f

        // Draw map
        canvas.drawRect(
            offsetX,
            offsetY,
            offsetX + mapLengthPixels,
            offsetY + mapWidthPixels,
            mapPaint
        )

        // Draw robot
        canvas.save()
        canvas.translate(offsetX + posX, offsetY + posY)
        canvas.rotate(theta)
        canvas.drawRect(
            -robotHalfWidth,
            -robotHalfHeight,
            robotHalfWidth,
            robotHalfHeight,
            paint
        )
        canvas.restore()
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = (event.x - lastTouchX) / scaleX
                val dy = (event.y - lastTouchY) / scaleY


                posX += dx
                posY += dy

                lastTouchX = event.x
                lastTouchY = event.y

                invalidate() // <- THIS redraws the view!
            }
        }
        return true
    }

    // Method to update robot position using odometry data
    fun updateOdometry(xMeters: Float, yMeters: Float, angleDegrees: Float) {
        // Convert meters to pixels
        posX = xMeters * metersToPixels
        posY = yMeters * metersToPixels
        theta = angleDegrees

        // Clamp to stay within map boundaries
        val minX = robotHalfWidth
        val maxX = mapLengthPixels - robotHalfWidth
        val minY = robotHalfHeight
        val maxY = mapWidthPixels - robotHalfHeight

        posX = posX.coerceIn(minX, maxX)
        posY = posY.coerceIn(minY, maxY)

        invalidate()
    }

}
