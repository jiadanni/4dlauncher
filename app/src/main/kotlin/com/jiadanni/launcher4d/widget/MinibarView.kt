package com.jiadanni.launcher4d.widget

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.ListView
import com.jiadanni.launcher4d.util.Tool
import kotlin.math.abs

class MinibarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ListView(context, attrs, defStyleAttr) {

    private lateinit var gestureDetector: GestureDetector
    var onSwipeRight: OnSwipeRight? = null

    init {
        init()
    }

    private fun init() {
        if (isInEditMode) return

        val dis = Tool.dp2px(10)
        val vDis = Tool.dp2px(30)
        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                e1 ?: return true
                val dx = (e2.x - e1.x).toInt()
                val dy = (e2.y - e1.y).toInt()
                if (abs(dx) > dis && abs(dy) < vDis) {
                    if (velocityX > 0) {
                        try {
                            val pos = pointToPosition(e1.x.toInt(), e1.y.toInt())
                            if (pos != -1) {
                                onSwipeRight?.onSwipe(pos, e1.x, e1.y)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                return true
            }
        })
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    interface OnSwipeRight {
        fun onSwipe(pos: Int, x: Float, y: Float)
    }
}
