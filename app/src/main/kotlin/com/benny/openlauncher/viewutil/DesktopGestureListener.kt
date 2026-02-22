package com.benny.openlauncher.viewutil

import com.benny.openlauncher.widget.Desktop
import `in`.championswimmer.sfg.lib.SimpleFingerGestures

class DesktopGestureListener(
    private val desktop: Desktop,
    private val callback: DesktopGestureCallback
) : SimpleFingerGestures.OnFingerGestureListener {

    enum class Type {
        SwipeUp,
        SwipeDown,
        SwipeLeft,
        SwipeRight,
        Pinch,
        Unpinch,
        DoubleTap
    }

    override fun onSwipeUp(i: Int, l: Long, v: Double): Boolean {
        return callback.onDrawerGesture(desktop, Type.SwipeUp)
    }

    override fun onSwipeDown(i: Int, l: Long, v: Double): Boolean {
        return callback.onDrawerGesture(desktop, Type.SwipeDown)
    }

    override fun onSwipeLeft(i: Int, l: Long, v: Double): Boolean {
        return callback.onDrawerGesture(desktop, Type.SwipeLeft)
    }

    override fun onSwipeRight(i: Int, l: Long, v: Double): Boolean {
        return callback.onDrawerGesture(desktop, Type.SwipeRight)
    }

    override fun onPinch(i: Int, l: Long, v: Double): Boolean {
        return callback.onDrawerGesture(desktop, Type.Pinch)
    }

    override fun onUnpinch(i: Int, l: Long, v: Double): Boolean {
        return callback.onDrawerGesture(desktop, Type.Unpinch)
    }

    override fun onDoubleTap(i: Int): Boolean {
        return callback.onDrawerGesture(desktop, Type.DoubleTap)
    }

    interface DesktopGestureCallback {
        fun onDrawerGesture(desktop: Desktop, event: Type): Boolean
    }
}
