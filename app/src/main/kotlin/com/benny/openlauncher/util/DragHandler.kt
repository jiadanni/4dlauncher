package com.benny.openlauncher.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import com.benny.openlauncher.activity.HomeActivity
import com.benny.openlauncher.manager.Setup
import com.benny.openlauncher.model.Item
import com.benny.openlauncher.viewutil.DesktopCallback
import com.benny.openlauncher.widget.AppItemView

object DragHandler {
    @JvmField
    var _cachedDragBitmap: Bitmap? = null

    @JvmStatic
    fun startDrag(view: View, item: Item, action: DragAction.Action, desktopCallback: DesktopCallback?) {
        _cachedDragBitmap = loadBitmapFromView(view)

        HomeActivity.launcher?.itemOptionView?.startDragNDropOverlay(view, item, action)
        desktopCallback?.setLastItem(item, view)
    }

    @JvmStatic
    fun getLongClick(item: Item, action: DragAction.Action, desktopCallback: DesktopCallback?): View.OnLongClickListener {
        return View.OnLongClickListener { view ->
            if (Setup.appSettings().desktopLock) {
                if (HomeActivity.launcher != null && action != DragAction.Action.SEARCH) {
                    if (Setup.appSettings().gestureFeedback) {
                        Tool.vibrate(view)
                    }
                    HomeActivity._launcher.itemOptionView.showItemPopupForLockedDesktop(item, HomeActivity.launcher)
                    return@OnLongClickListener true
                }
                return@OnLongClickListener false
            }
            if (Setup.appSettings().gestureFeedback) {
                Tool.vibrate(view)
            }
            startDrag(view, item, action, desktopCallback)
            true
        }
    }

    private fun loadBitmapFromView(view: View): Bitmap {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        var tempLabel: String? = null
        if (view is AppItemView) {
            tempLabel = view.label
            view.label = " "
        }

        view.layout(0, 0, view.width, view.height)
        view.draw(canvas)

        if (view is AppItemView) {
            view.label = tempLabel
        }

        view.parent.requestLayout()
        return bitmap
    }
}
