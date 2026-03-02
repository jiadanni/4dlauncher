package com.jiadanni.launcher4d.activity.homeparts

import android.content.Intent
import android.util.Log
import com.jiadanni.launcher4d.activity.HomeActivity
import com.jiadanni.launcher4d.manager.Setup
import com.jiadanni.launcher4d.util.AppSettings
import com.jiadanni.launcher4d.util.LauncherAction
import com.jiadanni.launcher4d.util.Tool
import com.jiadanni.launcher4d.viewutil.DesktopGestureListener
import com.jiadanni.launcher4d.widget.Desktop

class HpGestureCallback(
    private val appSettings: AppSettings
) : DesktopGestureListener.DesktopGestureCallback {

    override fun onDrawerGesture(desktop: Desktop, event: DesktopGestureListener.Type): Boolean {
        val gesture: Any? = when (event) {
            DesktopGestureListener.Type.SwipeUp -> appSettings.gestureSwipeUp
            DesktopGestureListener.Type.SwipeDown -> appSettings.gestureSwipeDown
            DesktopGestureListener.Type.SwipeLeft,
            DesktopGestureListener.Type.SwipeRight -> null
            DesktopGestureListener.Type.Pinch -> appSettings.gesturePinch
            DesktopGestureListener.Type.Unpinch -> appSettings.gestureUnpinch
            DesktopGestureListener.Type.DoubleTap -> appSettings.gestureDoubleTap
            else -> {
                Log.e(javaClass.toString(), "gesture error")
                null
            }
        }

        if (gesture != null) {
            if (appSettings.gestureFeedback) {
                Tool.vibrate(desktop)
            }
            when (gesture) {
                is Intent -> {
                    Tool.startApp(desktop.context, Setup.appLoader().findApp(gesture), null)
                }
                is LauncherAction.ActionDisplayItem -> {
                    LauncherAction.RunAction(gesture, desktop.context)
                }
            }
            return true
        }

        return false
    }
}
