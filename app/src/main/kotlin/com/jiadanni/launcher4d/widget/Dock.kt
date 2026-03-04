package com.jiadanni.launcher4d.widget

import android.content.Context
import android.graphics.Point
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.jiadanni.launcher4d.interfaces.Launcher
import com.jiadanni.launcher4d.manager.Setup
import com.jiadanni.launcher4d.model.Item
import com.jiadanni.launcher4d.util.Definitions.ItemPosition
import com.jiadanni.launcher4d.util.DragAction.Action
import com.jiadanni.launcher4d.util.DragHandler
import com.jiadanni.launcher4d.util.Tool
import com.jiadanni.launcher4d.viewutil.DesktopCallback
import com.jiadanni.launcher4d.viewutil.ItemViewFactory

class Dock(context: Context, attr: AttributeSet) : CellContainer(context, attr), DesktopCallback {

    private lateinit var launcher: Launcher
    private val coordinate = Point()
    private val previousDragPoint = Point()

    private var previousItem: Item? = null
    private var previousItemView: View? = null

    // open app drawer on slide up gesture
    private var startPosX: Float = 0f
    private var startPosY: Float = 0f

    fun initDock() {
        val columns = Setup.appSettings().dockColumnCount
        val rows = Setup.appSettings().dockRowCount
        setGridSize(columns, rows)
        val dockItems = Setup.dataManager().getDock()
        removeAllViews()
        for (item in dockItems) {
            if (item.x + item.spanX <= columns && item.y + item.spanY <= rows) {
                addItemToPage(item, 0)
            }
        }

        // call onMeasure to set the height
        measure(measuredWidth, measuredHeight)
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        detectSwipe(ev)
        super.dispatchTouchEvent(ev)
        return true
    }

    private fun detectSwipe(ev: MotionEvent) {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                startPosX = ev.x
                startPosY = ev.y
            }
            MotionEvent.ACTION_UP -> {
                if (startPosY - ev.y > 150.0f && Setup.appSettings().gestureDockSwipeUp) {
                    var point = Point(ev.x.toInt(), ev.y.toInt())
                    point = Tool.convertPoint(point, this, launcher.appDrawerController)
                    if (Setup.appSettings().gestureFeedback) {
                        Tool.vibrate(this)
                    }
                    launcher.openAppDrawer(this, point.x, point.y)
                }
            }
        }
    }

    fun updateIconProjection(x: Int, y: Int) {
        val dragNDropView = launcher.itemOptionView
        val state = peekItemAndSwap(x, y, coordinate)
        if (coordinate != previousDragPoint) {
            dragNDropView.cancelFolderPreview()
        }
        previousDragPoint.set(coordinate.x, coordinate.y)

        when (state) {
            DragState.CurrentNotOccupied -> {
                projectImageOutlineAt(coordinate, DragHandler._cachedDragBitmap)
            }
            DragState.CurrentOccupied -> {
                val type = dragNDropView.dragItem?.type
                clearCachedOutlineBitmap()
                if (type != Item.Type.WIDGET && coordinateToChildView(coordinate) is AppItemView) {
                    val yOffset = if (Setup.appSettings().dockShowLabel) Tool.dp2px(7) else 0
                    dragNDropView.showFolderPreviewAt(
                        this,
                        cellWidth * (coordinate.x + 0.5f),
                        cellHeight * (coordinate.y + 0.5f) - yOffset
                    )
                }
            }
            else -> {
                // OutOffRange, ItemViewNotFound
            }
        }
    }

    override fun setLastItem(item: Item, view: View) {
        previousItemView = view
        previousItem = item
        removeView(view)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (!isInEditMode) {
            // set the height for the dock based on the number of rows and the show label preference
            val iconSize = Setup.appSettings().dockIconSize
            var height = Tool.dp2px((iconSize + 20) * cellSpanV)
            if (Setup.appSettings().dockShowLabel) {
                height += Tool.dp2px(20)
            }
            layoutParams.height = height
            setMeasuredDimension(View.getDefaultSize(suggestedMinimumWidth, widthMeasureSpec), height)
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    override fun consumeLastItem() {
        previousItem = null
        previousItemView = null
    }

    override fun revertLastItem() {
        if (previousItemView != null && previousItem != null) {
            addViewToGrid(previousItemView!!)
            previousItem = null
            previousItemView = null
        }
    }

    override fun addItemToPage(item: Item, page: Int): Boolean {
        val itemView = ItemViewFactory.getItemView(context, this, Action.DESKTOP, item, isDockShowLabel())
        if (itemView == null) {
            // TODO see if this fixes SD card bug
            //Setup.dataManager().deleteItem(item, true)
            return false
        }
        item._location = ItemPosition.Dock
        addViewToGrid(itemView, item.x, item.y, item.spanX, item.spanY)
        return true
    }

    override fun addItemToPoint(item: Item, x: Int, y: Int): Boolean {
        val positionToLayoutPrams = coordinateToLayoutParams(x, y, item.spanX, item.spanY) ?: return false

        item._location = ItemPosition.Dock
        item.x = positionToLayoutPrams.x
        item.y = positionToLayoutPrams.y
        val itemView = ItemViewFactory.getItemView(context, this, Action.DESKTOP, item, isDockShowLabel())
        if (itemView != null) {
            itemView.layoutParams = positionToLayoutPrams
            addView(itemView)
        }
        return true
    }

    override fun addItemToCell(item: Item, x: Int, y: Int): Boolean {
        item._location = ItemPosition.Dock
        item.x = x
        item.y = y
        val itemView = ItemViewFactory.getItemView(context, this, Action.DESKTOP, item, isDockShowLabel())
            ?: return false

        addViewToGrid(itemView, item.x, item.y, item.spanX, item.spanY)
        return true
    }

    override fun removeItem(view: View, animate: Boolean) {
        if (animate) {
            view.animate().setDuration(100).scaleX(0.0f).scaleY(0.0f).withEndAction {
                if (view.parent == this@Dock) {
                    removeView(view)
                }
            }
        } else if (this == view.parent) {
            removeView(view)
        }
    }

    fun setHome(launcher: Launcher) {
        this.launcher = launcher
    }

    private fun isDockShowLabel(): Boolean = Setup.appSettings().dockShowLabel
}
