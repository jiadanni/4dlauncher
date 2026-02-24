package com.benny.openlauncher.activity.homeparts

import android.content.Intent
import android.util.Log
import com.benny.openlauncher.activity.HomeActivity
import com.benny.openlauncher.manager.Setup
import com.benny.openlauncher.util.AppSettings
import com.benny.openlauncher.util.LauncherAction
import com.benny.openlauncher.util.Tool
import com.benny.openlauncher.viewutil.DesktopGestureListener
import com.benny.openlauncher.widget.Desktop

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
