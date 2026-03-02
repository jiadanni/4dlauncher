package com.jiadanni.launcher4d.widget

import android.appwidget.AppWidgetHostView
import android.content.Context
import android.view.MotionEvent

class WidgetView(context: Context) : AppWidgetHostView(context) {
    private var onTouchListener: OnTouchListener? = null
    private var longClick: OnLongClickListener? = null
    private var down: Long = 0

    init {
        isLongClickable = true
    }

    override fun setOnTouchListener(onTouchListener: OnTouchListener?) {
        this.onTouchListener = onTouchListener
    }

    override fun setOnLongClickListener(l: OnLongClickListener?) {
        longClick = l
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        onTouchListener?.onTouch(this, ev)

        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                down = System.currentTimeMillis()
            }
            MotionEvent.ACTION_MOVE -> {
                val delta = System.currentTimeMillis() - down
                if (delta > 300L) {
                    longClick?.onLongClick(this)
                }
            }
        }

        return false
    }
}
