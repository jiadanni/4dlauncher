package com.benny.openlauncher.interfaces

import android.graphics.PointF
import android.view.View
import com.benny.openlauncher.model.Item
import com.benny.openlauncher.util.DragAction

interface DropTargetListener {
    fun getView(): View

    fun onStart(action: DragAction.Action, location: PointF, isInside: Boolean): Boolean

    fun onStartDrag(action: DragAction.Action, location: PointF)

    fun onDrop(action: DragAction.Action, location: PointF, item: Item)

    fun onMove(action: DragAction.Action, location: PointF)

    fun onEnter(action: DragAction.Action, location: PointF)

    fun onExit(action: DragAction.Action, location: PointF)

    fun onEnd()
}
