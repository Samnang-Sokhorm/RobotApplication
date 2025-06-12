package com.example.myapplication1

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.core.view.KeyEventDispatcher
import kotlin.math.abs

class MapView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : View(context, attrs, defStyle) {

    private val paintMap = Paint().apply { color = Color.LTGRAY; style = Paint.Style.FILL }
    private val paintUnit = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }
    private val paintGrid = Paint().apply { color = Color.GRAY; style = Paint.Style.STROKE; strokeWidth = 2f }
    private val paintObstacle = Paint().apply { color = Color.RED; style = Paint.Style.FILL }
    private val paintText = Paint().apply { color = Color.DKGRAY; textSize = 30f }

    private val unitSize = 50f
    private val mapWidthMeters = 14.80f
    private val mapHeightMeters = 6f
    private var scaleX = 1f
    private var scaleY = 1f

    private var defaultX = 2.0f
    private var defaultY = 2.0f
    private var unitX = defaultX
    private var unitY = defaultY

    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private var onLocationChange: ((Float, Float) -> Unit)? = null

    private val obstacles = listOf(
        RectF(), // Obstacle 1
        RectF() // Obstacle 2
    )

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        scaleX = w / mapWidthMeters
        scaleY = h / mapHeightMeters
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw map background
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paintMap)

        // Draw grid lines
        for (i in 1 until mapWidthMeters.toInt()) {
            canvas.drawLine(i * scaleX, 0f, i * scaleX, height.toFloat(), paintGrid)
        }
        for (i in 1 until mapHeightMeters.toInt()) {
            canvas.drawLine(0f, i * scaleY, width.toFloat(), i * scaleY, paintGrid)
        }

        // Draw obstacles
        // Convert unit position to pixels
        val pixelX = unitX * scaleX
        val pixelY = unitY * scaleY

        // Draw the unit
        canvas.drawCircle(pixelX, pixelY, unitSize, paintUnit)

        // Draw X, Y labels
        canvas.drawText("X: ${unitX.toInt()}m", pixelX + 20, pixelY, paintText)
        canvas.drawText("Y: ${unitY.toInt()}m", pixelX + 20, pixelY + 40, paintText)
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

                moveUnit(dx, dy)

                lastTouchX = event.x
                lastTouchY = event.y
            }
        }
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val moveSpeed = 0.2f // Adjust movement speed
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> moveUnit(0f, -moveSpeed)
            KeyEvent.KEYCODE_DPAD_DOWN -> moveUnit(0f, moveSpeed)
            KeyEvent.KEYCODE_DPAD_LEFT -> moveUnit(-moveSpeed, 0f)
            KeyEvent.KEYCODE_DPAD_RIGHT -> moveUnit(moveSpeed, 0f)
            KeyEvent.KEYCODE_BUTTON_A -> resetUnit() // A button resets position
            else -> return super.onKeyDown(keyCode, event)
        }
        return true
    }

    fun moveUnit(deltaX: Float, deltaY: Float) {
        val newX = unitX + deltaX
        val newY = unitY + deltaY

        if (!isColliding(newX, newY)) {
            unitX = newX.coerceIn(0f, mapWidthMeters)
            unitY = newY.coerceIn(0f, mapHeightMeters)

            onLocationChange?.invoke(unitX, unitY)
            invalidate()
        }
    }

    private fun isColliding(x: Float, y: Float): Boolean {
        return obstacles.any { obstacle ->
            x >= obstacle.left && x <= obstacle.right &&
                    y >= obstacle.top && y <= obstacle.bottom
        }
    }

    fun resetUnit() {
        unitX = defaultX
        unitY = defaultY
        onLocationChange?.invoke(unitX, unitY)
        invalidate()
    }

    fun setOnLocationChangeListener(listener: (Float, Float) -> Unit) {
        onLocationChange = listener
    }
}