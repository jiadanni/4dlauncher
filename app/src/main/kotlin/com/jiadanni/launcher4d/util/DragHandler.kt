package com.jiadanni.launcher4d.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import com.jiadanni.launcher4d.activity.HomeActivity
import com.jiadanni.launcher4d.manager.Setup
import com.jiadanni.launcher4d.model.Item
import com.jiadanni.launcher4d.viewutil.DesktopCallback
import com.jiadanni.launcher4d.widget.AppItemView

object DragHandler {
    @JvmField
    var _cachedDragBitmap: Bitmap? = null

    @JvmStatic
    fun startDrag(view: View, item: Item, action: DragAction.Action, desktopCallback: DesktopCallback?) {
        _cachedDragBitmap = loadBitmapFromView(view)

        Tool.getLauncher(view.context)?.itemOptionView?.startDragNDropOverlay(view, item, action)
        desktopCallback?.setLastItem(item, view)
    }

    @JvmStatic
    @JvmOverloads
    fun getLongClick(item: Item, action: DragAction.Action, desktopCallback: DesktopCallback?, dragView: View? = null): View.OnLongClickListener {
        return View.OnLongClickListener { view ->
            if (Setup.appSettings().desktopLock) {
                val launcher = Tool.getLauncher(view.context)
                if (launcher != null && action != DragAction.Action.SEARCH) {
                    if (Setup.appSettings().gestureFeedback) {
                        Tool.vibrate(view)
                    }
                    launcher.itemOptionView.showItemPopupForLockedDesktop(item, launcher as HomeActivity)
                    return@OnLongClickListener true
                }
                return@OnLongClickListener false
            }
            if (Setup.appSettings().gestureFeedback) {
                Tool.vibrate(view)
            }
            startDrag(dragView ?: view, item, action, desktopCallback)
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
