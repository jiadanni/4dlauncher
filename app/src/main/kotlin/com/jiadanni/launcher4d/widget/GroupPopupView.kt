package com.jiadanni.launcher4d.widget

import android.animation.Animator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import com.jiadanni.launcher4d.R
import com.jiadanni.launcher4d.activity.HomeActivity
import com.jiadanni.launcher4d.manager.Setup
import com.jiadanni.launcher4d.model.App
import com.jiadanni.launcher4d.model.Item
import com.jiadanni.launcher4d.util.AppSettings
import com.jiadanni.launcher4d.util.Definitions.ItemPosition
import com.jiadanni.launcher4d.util.Definitions.ItemState
import com.jiadanni.launcher4d.util.DragAction
import com.jiadanni.launcher4d.util.DragHandler
import com.jiadanni.launcher4d.util.Tool
import com.jiadanni.launcher4d.viewutil.DesktopCallback
import com.jiadanni.launcher4d.viewutil.GroupDrawable
import com.jiadanni.launcher4d.viewutil.ItemViewFactory
import net.gsantner.opoc.util.ContextUtils

class GroupPopupView : FrameLayout {
    private var _isShowing = false
    private lateinit var _popupCard: CardView
    private lateinit var _cellContainer: CellContainer
    private var _dismissListener: PopupWindow.OnDismissListener? = null
    private var _folderAnimator: Animator? = null
    private var _cx = 0
    private var _cy = 0
    private lateinit var _textViewGroupName: TextView

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
        if (isInEditMode) {
            return
        }
        _popupCard = LayoutInflater.from(context).inflate(R.layout.view_group_popup, this, false) as CardView
        // set the CardView color
        val color = Setup.appSettings().desktopFolderColor
        val alpha = Color.alpha(color)
        _popupCard.setCardBackgroundColor(color)
        // remove elevation if CardView's background is transparent to avoid weird shadows because CardView does not support transparent backgrounds
        if (alpha == 0) {
            _popupCard.cardElevation = 0f
        }
        _cellContainer = _popupCard.findViewById(R.id.group)

        bringToFront()

        setOnClickListener {
            _dismissListener?.onDismiss()
            collapse()
        }

        addView(_popupCard)
        _popupCard.visibility = View.INVISIBLE
        visibility = View.INVISIBLE

        _textViewGroupName = _popupCard.findViewById(R.id.group_popup_label)
    }

    fun showPopup(item: Item, itemView: View, callback: DesktopCallback): Boolean {
        if (_isShowing || visibility == View.VISIBLE) return false
        _isShowing = true

        val cu = ContextUtils(_textViewGroupName.context)
        val label = item.label
        _textViewGroupName.visibility = if (label.isEmpty()) GONE else VISIBLE
        _textViewGroupName.text = label
        _textViewGroupName.setTextColor(
            if (cu.shouldColorOnTopBeLight(Setup.appSettings().desktopFolderColor)) Color.WHITE else Color.BLACK
        )
        _textViewGroupName.setTypeface(null, Typeface.BOLD)
        cu.freeContextRef()

        val context = itemView.context
        val cellSize = GroupDef.getCellSize(item.groupItems.size)
        _cellContainer.setGridSize(cellSize[0], cellSize[1])

        val iconSize = Tool.dp2px(Setup.appSettings().desktopIconSize)
        val textSize = Tool.dp2px(22)
        val contentPadding = Tool.dp2px(6)

        var appsChanged = false

        for (x2 in 0 until cellSize[0]) {
            for (y2 in 0 until cellSize[1]) {
                if (y2 * cellSize[0] + x2 > item.groupItems.size - 1) {
                    continue
                }
                val groupItem = item.groupItems[y2 * cellSize[0] + x2] ?: continue
                val app = Setup.appLoader().findItemApp(groupItem)
                if (app == null || AppSettings.get().hiddenAppsList.contains(app.componentName)) {
                    deleteItem(context, item, groupItem, itemView as AppItemView)
                    appsChanged = true
                    continue
                } else {
                    val view = ItemViewFactory.getItemView(context, callback, DragAction.Action.DESKTOP, groupItem)
                    view.setOnLongClickListener {
                        if (Setup.appSettings().desktopLock) {
                            val launcher = Tool.getLauncher(context)
                            if (launcher != null) {
                                launcher.itemOptionView.showItemPopupForLockedDesktop(
                                    groupItem,
                                    launcher as HomeActivity
                                )
                                return@setOnLongClickListener true
                            }
                            false
                        } else {
                            removeItem(context, item, groupItem, itemView as AppItemView)

                            // start the drag action
                            DragHandler.startDrag(view, groupItem, DragAction.Action.DESKTOP, null)

                            collapse()

                            // update group icon or
                            // convert group item into app item if there is only one item left
                            updateItem(callback, item, itemView)
                            true
                        }
                    }
                    view.setOnClickListener {
                        Tool.createScaleInScaleOutAnim(view) {
                            collapse()
                            visibility = View.INVISIBLE
                            view.context.startActivity(groupItem.intent)
                        }
                    }
                    _cellContainer.addViewToGrid(view, x2, y2, 1, 1)
                }
            }
        }

        _dismissListener = PopupWindow.OnDismissListener {
            if ((itemView as AppItemView).icon != null) {
                ((itemView as AppItemView).icon as GroupDrawable).popBack()
            }
        }

        val popupWidth = contentPadding * 8 + _popupCard.contentPaddingLeft + _popupCard.contentPaddingRight + iconSize * cellSize[0]
        _popupCard.layoutParams.width = popupWidth

        val popupHeight = contentPadding * 2 + _popupCard.contentPaddingTop + _popupCard.contentPaddingBottom + Tool.dp2px(30) + (iconSize + textSize) * cellSize[1]
        _popupCard.layoutParams.height = popupHeight

        _cx = popupWidth / 2
        _cy = popupHeight / 2 - if (Setup.appSettings().desktopShowLabel) Tool.dp2px(10) else 0

        val coordinates = IntArray(2)
        itemView.getLocationInWindow(coordinates)

        coordinates[0] += itemView.width / 2
        coordinates[1] += itemView.height / 2

        coordinates[0] -= popupWidth / 2
        coordinates[1] -= popupHeight / 2

        val width = width
        val height = height

        if (coordinates[0] + popupWidth > width) {
            val v = width - (coordinates[0] + popupWidth)
            coordinates[0] += v
            coordinates[0] -= contentPadding
            _cx -= v
            _cx += contentPadding
        }
        if (coordinates[1] + popupHeight > height) {
            coordinates[1] += height - (coordinates[1] + popupHeight)
        }
        if (coordinates[0] < 0) {
            coordinates[0] -= itemView.width / 2
            coordinates[0] += popupWidth / 2
            coordinates[0] += contentPadding
            _cx += itemView.width / 2
            _cx -= popupWidth / 2
            _cx -= contentPadding
        }
        if (coordinates[1] < 0) {
            coordinates[1] -= itemView.height / 2
            coordinates[1] += popupHeight / 2
        }

        if (item._location == ItemPosition.Dock) {
            coordinates[1] -= iconSize / 2
            _cy += iconSize / 2 + if (Setup.appSettings().dockShowLabel) 0 else Tool.dp2px(10)
        }

        val x = coordinates[0]
        val y = coordinates[1]

        _popupCard.pivotX = 0f
        _popupCard.pivotY = 0f
        _popupCard.x = x.toFloat()
        _popupCard.y = y.toFloat()

        visibility = View.VISIBLE
        _popupCard.visibility = View.VISIBLE
        expand()

        if (appsChanged) {
            updateItem(callback, item, itemView)
            Toast.makeText(context, R.string.toast_update_group_due_to_missing_items, Toast.LENGTH_LONG).show()
        }

        return true
    }

    private fun expand() {
        _cellContainer.alpha = 0f

        val finalRadius = maxOf(_popupCard.width, _popupCard.height)
        val startRadius = Tool.dp2px(Setup.appSettings().desktopIconSize / 2)

        val animDuration = Setup.appSettings().animationSpeed * 10L
        _folderAnimator = android.view.ViewAnimationUtils.createCircularReveal(_popupCard, _cx, _cy, startRadius.toFloat(), finalRadius.toFloat())
        _folderAnimator?.startDelay = 0
        _folderAnimator?.interpolator = AccelerateDecelerateInterpolator()
        _folderAnimator?.duration = animDuration
        _folderAnimator?.start()
        Tool.visibleViews(animDuration, _cellContainer)
    }

    fun collapse() {
        if (!_isShowing) return
        if (_folderAnimator == null || _folderAnimator?.isRunning == true)
            return

        val animDuration = Setup.appSettings().animationSpeed * 10L
        Tool.invisibleViews(animDuration, _cellContainer)

        val startRadius = Tool.dp2px(Setup.appSettings().desktopIconSize / 2)
        val finalRadius = maxOf(_popupCard.width, _popupCard.height)
        _folderAnimator = android.view.ViewAnimationUtils.createCircularReveal(_popupCard, _cx, _cy, finalRadius.toFloat(), startRadius.toFloat())
        _folderAnimator?.startDelay = 1 + animDuration / 2
        _folderAnimator?.interpolator = AccelerateDecelerateInterpolator()
        _folderAnimator?.duration = animDuration
        _folderAnimator?.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(p1: Animator) {
            }

            override fun onAnimationEnd(p1: Animator) {
                _popupCard.visibility = View.INVISIBLE
                _isShowing = false

                _dismissListener?.onDismiss()

                _cellContainer.removeAllViews()
                visibility = View.INVISIBLE
            }

            override fun onAnimationCancel(p1: Animator) {
            }

            override fun onAnimationRepeat(p1: Animator) {
            }
        })
        _folderAnimator?.start()
    }

    private fun removeItem(context: Context, currentItem: Item, dragOutItem: Item, currentView: AppItemView) {
        currentItem.groupItems.remove(dragOutItem)

        Setup.dataManager().saveItem(dragOutItem, ItemState.Visible)
        Setup.dataManager().saveItem(currentItem)

        currentView.icon = GroupDrawable(context, currentItem, Setup.appSettings().desktopIconSize)
    }

    private fun deleteItem(context: Context, currentItem: Item, dragOutItem: Item, currentView: AppItemView) {
        currentItem.groupItems.remove(dragOutItem)

        Setup.dataManager().deleteItem(dragOutItem, false)
        Setup.dataManager().saveItem(currentItem)

        currentView.icon = GroupDrawable(context, currentItem, Setup.appSettings().desktopIconSize)
    }

    fun updateItem(callback: DesktopCallback, currentItem: Item, currentView: View) {
        if (currentItem.groupItems.size == 1) {
            val app = Setup.appLoader().findItemApp(currentItem.groupItems[0])
            if (app != null) {
                val item = Setup.dataManager().getItem(currentItem.groupItems[0].id)
                item.x = currentItem.x
                item.y = currentItem.y
                item._location = ItemPosition.Desktop

                // update db
                Setup.dataManager().saveItem(item)
                val launcher = Tool.getLauncher(currentView.context)
                if (launcher != null) {
                    Setup.dataManager().saveItem(item, launcher.desktop.currentItem, ItemPosition.Desktop)
                }
                Setup.dataManager().saveItem(item, ItemState.Visible)
                Setup.dataManager().deleteItem(currentItem, false)

                // update launcher
                callback.removeItem(currentView, false)
                callback.addItemToCell(item, item.x, item.y)
            }
        } else {
            callback.removeItem(currentView, false)
            callback.addItemToCell(currentItem, currentItem.x, currentItem.y)
        }
    }

    object GroupDef {
        const val _maxItem = 12

        fun getCellSize(count: Int): IntArray {
            if (count <= 1)
                return intArrayOf(1, 1)
            if (count <= 2)
                return intArrayOf(2, 1)
            if (count <= 4)
                return intArrayOf(2, 2)
            if (count <= 6)
                return intArrayOf(3, 2)
            if (count <= 9)
                return intArrayOf(3, 3)
            if (count <= 12)
                return intArrayOf(4, 3)
            return intArrayOf(0, 0)
        }
    }
}
