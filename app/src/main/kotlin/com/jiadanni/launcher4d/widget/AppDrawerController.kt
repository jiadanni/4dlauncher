package com.jiadanni.launcher4d.widget

import android.animation.Animator
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.WindowInsets
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import com.jiadanni.launcher4d.R
import com.jiadanni.launcher4d.manager.Setup
import net.gsantner.opoc.util.Callback
import kotlin.math.max

class AppDrawerController @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle) {

    var _drawerViewPage: AppDrawerPage? = null
    var _drawerViewGrid: AppDrawerGrid? = null
    var _drawerMode: Int = 0
    var _isOpen = false

    private var appDrawerCallback: Callback.a2<Boolean, Boolean>? = null
    private var appDrawerAnimator: Animator? = null
    private var drawerAnimationTime: Int = 0

    object Mode {
        const val LIST = 0
        const val GRID = 1
        const val PAGE = 2
    }

    fun setCallBack(callBack: Callback.a2<Boolean, Boolean>) {
        appDrawerCallback = callBack
    }

    val drawer: View
        get() = when (_drawerMode) {
            Mode.GRID -> _drawerViewGrid!!
            else -> _drawerViewPage!!
        }

    fun open(cx: Int, cy: Int) {
        if (_isOpen) return
        _isOpen = true

        drawerAnimationTime = Setup.appSettings().animationSpeed * 10
        appDrawerAnimator = android.view.ViewAnimationUtils.createCircularReveal(
            drawer, cx, cy, 0f, max(width, height).toFloat()
        )
        appDrawerAnimator?.interpolator = AccelerateDecelerateInterpolator()
        appDrawerAnimator?.duration = drawerAnimationTime.toLong()
        appDrawerAnimator?.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(p1: Animator) {
                visibility = VISIBLE
                appDrawerCallback?.callback(true, true)
            }

            override fun onAnimationEnd(p1: Animator) {
                appDrawerCallback?.callback(true, false)
            }

            override fun onAnimationCancel(p1: Animator) {
            }

            override fun onAnimationRepeat(p1: Animator) {
            }
        })

        appDrawerAnimator?.start()
    }

    fun close(cx: Int, cy: Int) {
        if (!_isOpen) return
        _isOpen = false

        drawerAnimationTime = Setup.appSettings().animationSpeed * 10
        appDrawerAnimator = android.view.ViewAnimationUtils.createCircularReveal(
            drawer, cx, cy, max(width, height).toFloat(), 0f
        )
        appDrawerAnimator?.interpolator = AccelerateDecelerateInterpolator()
        appDrawerAnimator?.duration = drawerAnimationTime.toLong()
        appDrawerAnimator?.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(p1: Animator) {
                appDrawerCallback?.callback(false, true)
            }

            override fun onAnimationEnd(p1: Animator) {
                appDrawerCallback?.callback(false, false)
                visibility = GONE
            }

            override fun onAnimationCancel(p1: Animator) {
            }

            override fun onAnimationRepeat(p1: Animator) {
            }
        })

        appDrawerAnimator?.start()
    }

    fun reset() {
        when (_drawerMode) {
            Mode.GRID -> _drawerViewGrid?.recyclerView?.scrollToPosition(0)
            else -> _drawerViewPage?.setCurrentItem(0, false)
        }
    }

    fun init() {
        if (isInEditMode) return

        val layoutInflater = LayoutInflater.from(context)
        _drawerMode = Setup.appSettings().drawerStyle
        visibility = GONE
        setBackgroundColor(Setup.appSettings().drawerBackgroundColor)

        when (_drawerMode) {
            Mode.GRID -> {
                _drawerViewGrid = AppDrawerGrid(context)
                addView(_drawerViewGrid)
            }
            else -> {
                _drawerViewPage = layoutInflater.inflate(R.layout.view_app_drawer_page, this, false) as AppDrawerPage
                addView(_drawerViewPage)
                val indicator = layoutInflater.inflate(R.layout.view_drawer_indicator, this, false) as PagerIndicator
                addView(indicator)
                _drawerViewPage?.withHome(indicator)
            }
        }
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            setPadding(0, insets.systemWindowInsetTop, 0, insets.systemWindowInsetBottom)
            return insets
        }
        return insets
    }
}
