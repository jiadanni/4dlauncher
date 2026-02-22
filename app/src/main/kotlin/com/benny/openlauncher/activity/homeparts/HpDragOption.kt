package com.benny.openlauncher.activity.homeparts

import android.graphics.Point
import android.graphics.PointF
import android.os.Handler
import android.view.View
import com.benny.openlauncher.R
import com.benny.openlauncher.activity.HomeActivity
import com.benny.openlauncher.interfaces.DropTargetListener
import com.benny.openlauncher.manager.Setup
import com.benny.openlauncher.model.Item
import com.benny.openlauncher.util.Definitions
import com.benny.openlauncher.util.DragAction
import com.benny.openlauncher.util.DragAction.Action
import com.benny.openlauncher.util.Tool
import com.benny.openlauncher.widget.Desktop
import com.benny.openlauncher.widget.ItemOptionView

class HpDragOption {
    fun initDragNDrop(
        homeActivity: HomeActivity,
        leftDragHandle: View,
        rightDragHandle: View,
        dragNDropView: ItemOptionView
    ) {
        val dragHandler = Handler()

        // Left drag handle
        dragNDropView.registerDropTarget(object : DropTargetListener {
            val runnable = object : Runnable {
                override fun run() {
                    val i = homeActivity.desktop.currentItem
                    if (i > 0) {
                        homeActivity.desktop.currentItem = i - 1
                    } else if (i == 0) {
                        homeActivity.desktop.addPageLeft(true)
                    }
                    dragHandler.postDelayed(this, 1000)
                }
            }

            override fun getView(): View = leftDragHandle

            override fun onStart(action: Action, location: PointF, isInside: Boolean) = true

            override fun onStartDrag(action: Action, location: PointF) {
                leftDragHandle.animate().alpha(0.5f)
            }

            override fun onEnter(action: Action, location: PointF) {
                dragHandler.post(runnable)
                leftDragHandle.animate().alpha(0.9f)
            }

            override fun onMove(action: Action, location: PointF) {
                // do nothing
            }

            override fun onDrop(action: Action, location: PointF, item: Item) {
                // do nothing
            }

            override fun onExit(action: Action, location: PointF) {
                dragHandler.removeCallbacksAndMessages(null)
                leftDragHandle.animate().alpha(0.5f)
            }

            override fun onEnd() {
                dragHandler.removeCallbacksAndMessages(null)
                leftDragHandle.animate().alpha(0f)
            }
        })

        // Right drag handle
        dragNDropView.registerDropTarget(object : DropTargetListener {
            val runnable = object : Runnable {
                override fun run() {
                    val i = homeActivity.desktop.currentItem
                    if (i < homeActivity.desktop.pages.size - 1) {
                        homeActivity.desktop.currentItem = i + 1
                    } else if (i == homeActivity.desktop.pages.size - 1) {
                        homeActivity.desktop.addPageRight(true)
                    }
                    dragHandler.postDelayed(this, 1000)
                }
            }

            override fun getView(): View = rightDragHandle

            override fun onStart(action: Action, location: PointF, isInside: Boolean) = true

            override fun onStartDrag(action: Action, location: PointF) {
                rightDragHandle.animate().alpha(0.5f)
            }

            override fun onEnter(action: Action, location: PointF) {
                dragHandler.post(runnable)
                rightDragHandle.animate().alpha(0.9f)
            }

            override fun onMove(action: Action, location: PointF) {
                // do nothing
            }

            override fun onDrop(action: Action, location: PointF, item: Item) {
                // do nothing
            }

            override fun onExit(action: Action, location: PointF) {
                dragHandler.removeCallbacksAndMessages(null)
                rightDragHandle.animate().alpha(0.5f)
            }

            override fun onEnd() {
                dragHandler.removeCallbacksAndMessages(null)
                rightDragHandle.animate().alpha(0f)
            }
        })

        // Desktop drag event
        dragNDropView.registerDropTarget(object : DropTargetListener {
            override fun getView(): View = homeActivity.desktop

            override fun onStart(action: Action, location: PointF, isInside: Boolean): Boolean {
                if (DragAction.Action.SEARCH != action) {
                    homeActivity.itemOptionView.showItemPopup(homeActivity)
                }
                return true
            }

            override fun onStartDrag(action: Action, location: PointF) {
                homeActivity.closeAppDrawer()
                homeActivity.searchBar.collapse()
                if (Setup.appSettings().desktopShowGrid) {
                    homeActivity.dock.setHideGrid(false)
                    for (cellContainer in homeActivity.desktop.pages) {
                        cellContainer.setHideGrid(false)
                    }
                }
            }

            override fun onEnter(action: Action, location: PointF) {
                // do nothing
            }

            override fun onDrop(action: Action, location: PointF, item: Item) {
                // this statement makes sure that adding an app multiple times from the app drawer works
                // the app will get a new id every time
                if (DragAction.Action.DRAWER == action) {
                    if (homeActivity.appDrawerController._isOpen) {
                        return
                    }
                    item.reset()
                }

                val x = location.x.toInt()
                val y = location.y.toInt()
                if (homeActivity.desktop.addItemToPoint(item, x, y)) {
                    homeActivity.desktop.consumeLastItem()
                    homeActivity.dock.consumeLastItem()
                    // add the item to the database
                    HomeActivity._db.saveItem(item, homeActivity.desktop.currentItem, Definitions.ItemPosition.Desktop)
                    homeActivity.desktop.updateDesktop()
                } else {
                    val pos = Point()
                    homeActivity.desktop.currentPage.touchPosToCoordinate(pos, x, y, item._spanX, item._spanY, false)
                    val itemView = homeActivity.desktop.currentPage.coordinateToChildView(pos)
                    if (itemView != null && Desktop.handleOnDropOver(
                            homeActivity,
                            item,
                            itemView.tag as Item,
                            itemView,
                            homeActivity.desktop.currentPage,
                            homeActivity.desktop.currentItem,
                            Definitions.ItemPosition.Desktop,
                            homeActivity.desktop
                        )
                    ) {
                        homeActivity.desktop.consumeLastItem()
                        homeActivity.dock.consumeLastItem()
                    } else {
                        Tool.toast(homeActivity, R.string.toast_not_enough_space)
                        homeActivity.desktop.revertLastItem()
                        homeActivity.dock.revertLastItem()
                    }
                }
            }

            override fun onMove(action: Action, location: PointF) {
                homeActivity.desktop.updateIconProjection(location.x.toInt(), location.y.toInt())
            }

            override fun onExit(action: Action, location: PointF) {
                for (page in homeActivity.desktop.pages) {
                    page.clearCachedOutlineBitmap()
                }
                dragNDropView.cancelFolderPreview()
            }

            override fun onEnd() {
                for (page in homeActivity.desktop.pages) {
                    page.clearCachedOutlineBitmap()
                }
                if (Setup.appSettings().desktopShowGrid) {
                    homeActivity.dock.setHideGrid(true)
                    for (cellContainer in homeActivity.desktop.pages) {
                        cellContainer.setHideGrid(true)
                    }
                }
            }
        })

        // Dock drag event
        dragNDropView.registerDropTarget(object : DropTargetListener {
            override fun getView(): View = homeActivity.dock

            override fun onStart(action: Action, location: PointF, isInside: Boolean) = true

            override fun onStartDrag(action: Action, location: PointF) {
                // do nothing
            }

            override fun onDrop(action: Action, location: PointF, item: Item) {
                if (DragAction.Action.DRAWER == action) {
                    if (homeActivity.appDrawerController._isOpen) {
                        return
                    }
                    item.reset()
                }

                val x = location.x.toInt()
                val y = location.y.toInt()
                if (homeActivity.dock.addItemToPoint(item, x, y)) {
                    homeActivity.desktop.consumeLastItem()
                    homeActivity.dock.consumeLastItem()

                    // add the item to the database
                    HomeActivity._db.saveItem(item, 0, Definitions.ItemPosition.Dock)
                } else {
                    val pos = Point()
                    homeActivity.dock.touchPosToCoordinate(pos, x, y, item._spanX, item._spanY, false)
                    val itemView = homeActivity.dock.coordinateToChildView(pos)
                    if (itemView != null) {
                        if (Desktop.handleOnDropOver(
                                homeActivity,
                                item,
                                itemView.tag as Item,
                                itemView,
                                homeActivity.dock,
                                0,
                                Definitions.ItemPosition.Dock,
                                homeActivity.dock
                            )
                        ) {
                            homeActivity.desktop.consumeLastItem()
                            homeActivity.dock.consumeLastItem()
                        } else {
                            Tool.toast(homeActivity, R.string.toast_not_enough_space)
                            homeActivity.desktop.revertLastItem()
                            homeActivity.dock.revertLastItem()
                        }
                    } else {
                        Tool.toast(homeActivity, R.string.toast_not_enough_space)
                        homeActivity.desktop.revertLastItem()
                        homeActivity.dock.revertLastItem()
                    }
                }
            }

            override fun onMove(action: Action, location: PointF) {
                homeActivity.dock.updateIconProjection(location.x.toInt(), location.y.toInt())
            }

            override fun onEnter(action: Action, location: PointF) {
                // do nothing
            }

            override fun onExit(action: Action, location: PointF) {
                homeActivity.dock.clearCachedOutlineBitmap()
                dragNDropView.cancelFolderPreview()
            }

            override fun onEnd() {
                homeActivity.dock.clearCachedOutlineBitmap()
            }
        })
    }
}
