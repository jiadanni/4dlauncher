package com.benny.openlauncher.viewutil

import android.content.Context
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Multi-touch gesture detector for advanced gestures
 * Detects: pinch in/out, two-finger scroll, two-finger double-tap
 */
class MultiTouchGestureDetector(
    private val context: Context,
    private val listener: OnMultiTouchGestureListener
) {

    // Gesture state
    private var previousDistance: Float = 0f
    private var previousAngle: Float = 0f
    private var twoFingerStartY: Float = 0f
    private var twoFingerStartX: Float = 0f
    private var gestureInProgress = false
    private var gestureType: GestureType? = null

    // Two-finger tap detection
    private var twoFingerTapCount = 0
    private var lastTwoFingerTapTime: Long = 0
    private var twoFingerTapDetected = false

    // Thresholds
    private val pinchThreshold = 50f // Minimum distance change for pinch
    private val scrollThreshold = 100f // Minimum distance for scroll
    private val doubleTapTimeout = 300L // Max time between taps (ms)
    private val tapMovementThreshold = 30f // Max movement for tap

    private var initialTwoFingerDistance: Float = 0f
    private var twoFingerMoved = false

    enum class GestureType {
        PINCH_IN,
        PINCH_OUT,
        TWO_FINGER_SCROLL_UP,
        TWO_FINGER_SCROLL_DOWN,
        TWO_FINGER_DOUBLE_TAP
    }

    interface OnMultiTouchGestureListener {
        fun onPinchIn(): Boolean = false
        fun onPinchOut(): Boolean = false
        fun onTwoFingerScrollUp(): Boolean = false
        fun onTwoFingerScrollDown(): Boolean = false
        fun onTwoFingerDoubleTap(): Boolean = false
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        val pointerCount = event.pointerCount

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Single finger down - reset state
                reset()
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (pointerCount == 2) {
                    // Two fingers down
                    handleTwoFingerDown(event)
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (pointerCount == 2) {
                    handleTwoFingerMove(event)
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                if (pointerCount == 2) {
                    // Second finger lifted
                    handleTwoFingerUp(event)
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                reset()
            }
        }

        return gestureInProgress
    }

    private fun handleTwoFingerDown(event: MotionEvent) {
        twoFingerTapDetected = false
        twoFingerMoved = false

        // Calculate initial positions
        val x0 = event.getX(0)
        val y0 = event.getY(0)
        val x1 = event.getX(1)
        val y1 = event.getY(1)

        twoFingerStartX = (x0 + x1) / 2f
        twoFingerStartY = (y0 + y1) / 2f

        previousDistance = calculateDistance(event)
        initialTwoFingerDistance = previousDistance
        previousAngle = calculateAngle(event)
        gestureInProgress = false
        gestureType = null
    }

    private fun handleTwoFingerMove(event: MotionEvent) {
        val currentDistance = calculateDistance(event)
        val distanceChange = currentDistance - previousDistance

        // Calculate center point movement
        val x0 = event.getX(0)
        val y0 = event.getY(0)
        val x1 = event.getX(1)
        val y1 = event.getY(1)

        val currentCenterX = (x0 + x1) / 2f
        val currentCenterY = (y0 + y1) / 2f

        val deltaX = currentCenterX - twoFingerStartX
        val deltaY = currentCenterY - twoFingerStartY

        // Check if fingers moved significantly
        if (abs(deltaX) > tapMovementThreshold || abs(deltaY) > tapMovementThreshold) {
            twoFingerMoved = true
        }

        // Detect gesture type if not already in progress
        if (!gestureInProgress) {
            // Determine if it's a pinch or scroll
            val totalDistanceChange = abs(currentDistance - initialTwoFingerDistance)

            if (totalDistanceChange > pinchThreshold) {
                // It's a pinch gesture
                gestureType = if (distanceChange > 0) GestureType.PINCH_OUT else GestureType.PINCH_IN
                gestureInProgress = true
                triggerGesture(gestureType!!)
            } else if (abs(deltaY) > scrollThreshold && abs(deltaY) > abs(deltaX) * 1.5f) {
                // It's a scroll gesture (vertical movement dominant)
                gestureType = if (deltaY < 0) GestureType.TWO_FINGER_SCROLL_UP else GestureType.TWO_FINGER_SCROLL_DOWN
                gestureInProgress = true
                triggerGesture(gestureType!!)
            }
        }

        previousDistance = currentDistance
    }

    private fun handleTwoFingerUp(event: MotionEvent) {
        // Check for two-finger tap
        if (!twoFingerMoved && !gestureInProgress) {
            val currentTime = System.currentTimeMillis()

            if (currentTime - lastTwoFingerTapTime < doubleTapTimeout) {
                // Double tap detected
                twoFingerTapCount++
                if (twoFingerTapCount >= 2) {
                    triggerGesture(GestureType.TWO_FINGER_DOUBLE_TAP)
                    twoFingerTapCount = 0
                }
            } else {
                // First tap
                twoFingerTapCount = 1
            }

            lastTwoFingerTapTime = currentTime
        }

        gestureInProgress = false
        gestureType = null
    }

    private fun calculateDistance(event: MotionEvent): Float {
        if (event.pointerCount < 2) return 0f

        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)

        return sqrt(x * x + y * y)
    }

    private fun calculateAngle(event: MotionEvent): Float {
        if (event.pointerCount < 2) return 0f

        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)

        return Math.toDegrees(atan2(y.toDouble(), x.toDouble())).toFloat()
    }

    private fun triggerGesture(type: GestureType): Boolean {
        return when (type) {
            GestureType.PINCH_IN -> listener.onPinchIn()
            GestureType.PINCH_OUT -> listener.onPinchOut()
            GestureType.TWO_FINGER_SCROLL_UP -> listener.onTwoFingerScrollUp()
            GestureType.TWO_FINGER_SCROLL_DOWN -> listener.onTwoFingerScrollDown()
            GestureType.TWO_FINGER_DOUBLE_TAP -> listener.onTwoFingerDoubleTap()
        }
    }

    private fun reset() {
        previousDistance = 0f
        previousAngle = 0f
        gestureInProgress = false
        gestureType = null
        twoFingerMoved = false
    }

    companion object {
        /**
         * Create a simple listener that handles a specific gesture
         */
        fun createListener(
            onPinchIn: () -> Boolean = { false },
            onPinchOut: () -> Boolean = { false },
            onTwoFingerScrollUp: () -> Boolean = { false },
            onTwoFingerScrollDown: () -> Boolean = { false },
            onTwoFingerDoubleTap: () -> Boolean = { false }
        ): OnMultiTouchGestureListener {
            return object : OnMultiTouchGestureListener {
                override fun onPinchIn() = onPinchIn()
                override fun onPinchOut() = onPinchOut()
                override fun onTwoFingerScrollUp() = onTwoFingerScrollUp()
                override fun onTwoFingerScrollDown() = onTwoFingerScrollDown()
                override fun onTwoFingerDoubleTap() = onTwoFingerDoubleTap()
            }
        }
    }
}
