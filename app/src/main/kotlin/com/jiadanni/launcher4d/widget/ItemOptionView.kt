package com.jiadanni.launcher4d.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jiadanni.launcher4d.R
import com.jiadanni.launcher4d.activity.HomeActivity
import com.jiadanni.launcher4d.activity.homeparts.HpItemOption
import com.jiadanni.launcher4d.interfaces.DropTargetListener
import com.jiadanni.launcher4d.manager.Setup
import com.jiadanni.launcher4d.model.Item
import com.jiadanni.launcher4d.util.DragAction.Action
import com.jiadanni.launcher4d.util.DragHandler
import com.jiadanni.launcher4d.util.Tool
import com.jiadanni.launcher4d.viewutil.AbstractPopupIconLabelItem
import com.jiadanni.launcher4d.viewutil.PopupDynamicIconLabelItem
import com.jiadanni.launcher4d.viewutil.PopupIconLabelItem
import com.mikepenz.fastadapter.IAdapter
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter
import jp.wasabeef.recyclerview.animators.SlideInLeftAnimator
import jp.wasabeef.recyclerview.animators.SlideInRightAnimator

class ItemOptionView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    class DragFlag {
        var previousOutside: Boolean = true
        var shouldIgnore: Boolean = false
    }

    @SuppressLint("ResourceType")
    inner class OverlayView : View(this@ItemOptionView.context) {

        init {
            setWillNotDraw(false)
        }

        override fun onTouchEvent(event: MotionEvent?): Boolean {
            if (event == null || event.actionMasked != MotionEvent.ACTION_DOWN || dragging || !_overlayPopupShowing) {
                return super.onTouchEvent(event)
            }
            collapse()
            return true
        }

        override fun onDraw(canvas: Canvas?) {
            super.onDraw(canvas)
            if (canvas == null || DragHandler._cachedDragBitmap == null || _dragLocation.equals(-1f, -1f))
                return

            val x = _dragLocation.x
            val y = _dragLocation.y

            if (_dragging) {
                canvas.save()
                _overlayIconScale = Tool.clampFloat(_overlayIconScale + 0.05f, 1f, 1.1f)
                canvas.scale(
                    _overlayIconScale, _overlayIconScale,
                    x + DragHandler._cachedDragBitmap.width / 2,
                    y + DragHandler._cachedDragBitmap.height / 2
                )
                canvas.drawBitmap(
                    DragHandler._cachedDragBitmap,
                    x - DragHandler._cachedDragBitmap.width / 2,
                    y - DragHandler._cachedDragBitmap.height / 2,
                    _paint
                )
                canvas.restore()
            }

            if (_dragging)
                invalidate()
        }
    }

    private val DRAG_THRESHOLD = 20.0f
    private var _dragAction: Action? = null
    private var _dragExceedThreshold = false
    private var _dragItem: Item? = null
    private val _dragLocation = PointF()
    private val _dragLocationConverted = PointF()
    private val _dragLocationStart = PointF()
    private var _dragView: View? = null
    private var _dragging = false
    private var _folderPreviewScale = 0f
    private var _overlayIconScale = 1.0f
    private val _overlayPopup: RecyclerView
    private val _overlayPopupAdapter = FastItemAdapter<AbstractPopupIconLabelItem>()
    private var _overlayPopupShowing = false
    private val _overlayView: OverlayView
    private val _paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val _previewLocation = PointF()
    private val _registeredDropTargetEntries = HashMap<DropTargetListener, DragFlag>()
    private var _showFolderPreview = false
    private val _slideInLeftAnimator = SlideInLeftAnimator(AccelerateDecelerateInterpolator())
    private val _slideInRightAnimator = SlideInRightAnimator(AccelerateDecelerateInterpolator())
    private val _tempArrayOfInt2 = IntArray(2)

    private val uninstallItemIdentifier = 83
    private val infoItemIdentifier = 84
    private val editItemIdentifier = 85
    private val removeItemIdentifier = 86
    private val resizeItemIdentifier = 87
    private val startShortcutItemIdentifier = 88

    private val uninstallItem = PopupIconLabelItem(R.string.uninstall, R.drawable.ic_delete).withIdentifier(uninstallItemIdentifier.toLong())
    private val infoItem = PopupIconLabelItem(R.string.info, R.drawable.ic_info).withIdentifier(infoItemIdentifier.toLong())
    private val editItem = PopupIconLabelItem(R.string.edit, R.drawable.ic_edit).withIdentifier(editItemIdentifier.toLong())
    private val removeItem = PopupIconLabelItem(R.string.remove, R.drawable.ic_close).withIdentifier(removeItemIdentifier.toLong())
    private val resizeItem = PopupIconLabelItem(R.string.resize, R.drawable.ic_resize).withIdentifier(resizeItemIdentifier.toLong())

    init {
        _paint.isFilterBitmap = true
        _paint.color = Setup.appSettings().desktopFolderColor
        _overlayView = OverlayView()
        _overlayPopup = RecyclerView(context).apply {
            visibility = View.INVISIBLE
            alpha = 0f
            overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
            itemAnimator = _slideInLeftAnimator
            adapter = _overlayPopupAdapter
        }
        addView(_overlayView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(_overlayPopup, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
        setWillNotDraw(false)
    }

    val dragging: Boolean
        get() = _dragging

    val dragLocation: PointF
        get() = _dragLocation

    val dragAction: Action?
        get() = _dragAction

    val dragExceedThreshold: Boolean
        get() = _dragExceedThreshold

    val dragItem: Item?
        get() = _dragItem

    fun showFolderPreviewAt(fromView: View, x: Float, y: Float) {
        if (!_showFolderPreview) {
            _showFolderPreview = true
            convertPoint(fromView, this, x, y)
            _folderPreviewScale = 0.0f
            invalidate()
        }
    }

    fun convertPoint(fromView: View, toView: View, x: Float, y: Float) {
        val fromCoordinate = IntArray(2)
        val toCoordinate = IntArray(2)
        fromView.getLocationOnScreen(fromCoordinate)
        toView.getLocationOnScreen(toCoordinate)
        _previewLocation.set(
            fromCoordinate[0] - toCoordinate[0] + x,
            fromCoordinate[1] - toCoordinate[1] + y
        )
    }

    fun cancelFolderPreview() {
        _showFolderPreview = false
        _previewLocation.set(-1.0f, -1.0f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        if (canvas != null && _showFolderPreview && !_previewLocation.equals(-1.0f, -1.0f)) {
            _folderPreviewScale += 0.08f
            _folderPreviewScale = Tool.clampFloat(_folderPreviewScale, 0.5f, 1.0f)
            canvas.drawCircle(
                _previewLocation.x,
                _previewLocation.y,
                Tool.dp2px((Setup.appSettings().desktopIconSize / 2) + 10).toFloat() * _folderPreviewScale,
                _paint
            )
        }
        if (_showFolderPreview) {
            invalidate()
        }
    }

    override fun onViewAdded(child: View?) {
        super.onViewAdded(child)
        _overlayView.bringToFront()
        _overlayPopup.bringToFront()
    }

    fun showPopupMenuForItem(
        x: Float,
        y: Float,
        popupItem: List<AbstractPopupIconLabelItem>,
        listener: com.mikepenz.fastadapter.listeners.OnClickListener<AbstractPopupIconLabelItem>
    ) {
        if (!_overlayPopupShowing) {
            _overlayPopupShowing = true
            _overlayPopup.visibility = View.VISIBLE
            _overlayPopup.translationX = x
            _overlayPopup.translationY = y
            _overlayPopup.alpha = 1.0f
            _overlayPopupAdapter.add(popupItem)
            _overlayPopupAdapter.withOnClickListener(listener)
        }
    }

    fun setPopupMenuShowDirection(left: Boolean) {
        _overlayPopup.itemAnimator = if (left) {
            _slideInLeftAnimator
        } else {
            _slideInRightAnimator
        }
    }

    fun collapse() {
        if (_overlayPopupShowing) {
            _overlayPopupShowing = false
            _overlayPopup.animate().alpha(0.0f).withEndAction {
                _overlayPopup.visibility = View.INVISIBLE
                _overlayPopupAdapter.clear()
            }
            if (!_dragging) {
                _dragView = null
                _dragItem = null
                _dragAction = null
            }
        }
    }

    fun startDragNDropOverlay(view: View, item: Item, action: Action) {
        _dragging = true
        _dragExceedThreshold = false
        _overlayIconScale = 0.0f
        _dragView = view
        _dragItem = item
        _dragAction = action
        _dragLocationStart.set(_dragLocation)

        for ((dropTargetListener, dragFlag) in _registeredDropTargetEntries) {
            convertPoint(dropTargetListener.view)
            dragFlag.shouldIgnore = !dropTargetListener.onStart(
                _dragAction,
                _dragLocationConverted,
                isViewContains(dropTargetListener.view, _dragLocation.x.toInt(), _dragLocation.y.toInt())
            )
        }

        _overlayView.invalidate()
    }

    override fun onDetachedFromWindow() {
        cancelAllDragNDrop()
        super.onDetachedFromWindow()
    }

    fun cancelAllDragNDrop() {
        _dragging = false
        if (!_overlayPopupShowing) {
            _dragView = null
            _dragItem = null
            _dragAction = null
        }
        for ((dropTargetListener, _) in _registeredDropTargetEntries) {
            dropTargetListener.onEnd()
        }
    }

    fun registerDropTarget(targetListener: DropTargetListener) {
        _registeredDropTargetEntries[targetListener] = DragFlag()
    }

    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {
        if (event != null && event.actionMasked == MotionEvent.ACTION_UP && _dragging) {
            handleDragFinished()
        }
        if (_dragging) {
            return true
        }
        event?.let {
            _dragLocation.set(it.x, it.y)
        }
        return super.onInterceptTouchEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event?.let {
            if (_dragging) {
                _dragLocation.set(it.x, it.y)
                when (it.actionMasked) {
                    MotionEvent.ACTION_UP -> handleDragFinished()
                    MotionEvent.ACTION_MOVE -> handleMovement()
                }
                if (_dragging) {
                    return true
                }
                return super.onTouchEvent(event)
            }
        }
        return super.onTouchEvent(event)
    }

    fun showItemPopup(homeActivity: HomeActivity) {
        val itemList = ArrayList<AbstractPopupIconLabelItem>()
        when (dragItem?.type) {
            Item.Type.APP -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && dragItem?.shortcutInfo != null) {
                    dragItem?.shortcutInfo?.forEach { shortcutInfo ->
                        itemList.add(getAppShortcutItem(shortcutInfo))
                    }
                }
                if (dragAction != Action.DRAWER) {
                    itemList.add(editItem)
                    itemList.add(removeItem)
                }
                itemList.add(uninstallItem)
                itemList.add(infoItem)
            }
            Item.Type.SHORTCUT -> {
                if (dragAction != Action.DRAWER) {
                    itemList.add(editItem)
                    itemList.add(removeItem)
                }
                itemList.add(infoItem)
            }
            Item.Type.ACTION, Item.Type.GROUP -> {
                itemList.add(editItem)
                itemList.add(removeItem)
            }
            Item.Type.WIDGET -> {
                itemList.add(removeItem)
                itemList.add(resizeItem)
            }
            else -> {}
        }

        var x = dragLocation.x - HomeActivity._itemTouchX + Tool.dp2px(10)
        var y = dragLocation.y - HomeActivity._itemTouchY - Tool.dp2px(46 * itemList.size)

        if (x + Tool.dp2px(200) > width) {
            setPopupMenuShowDirection(false)
            x = dragLocation.x - HomeActivity._itemTouchX +
                    homeActivity.desktop.currentPage.cellWidth - Tool.dp2px(200) - Tool.dp2px(10)
        } else {
            setPopupMenuShowDirection(true)
        }

        if (y < 0) {
            y = dragLocation.y - HomeActivity._itemTouchY +
                    homeActivity.desktop.currentPage.cellHeight + Tool.dp2px(4)
        } else {
            y -= Tool.dp2px(4)
        }

        showPopupMenuForItem(x.toFloat(), y.toFloat(), itemList) { v, adapter, item, position ->
            dragItem?.let { dragItem ->
                val itemOption = HpItemOption(homeActivity)
                when (item.identifier.toInt()) {
                    uninstallItemIdentifier -> itemOption.onUninstallItem(dragItem)
                    editItemIdentifier -> itemOption.onEditItem(dragItem)
                    removeItemIdentifier -> itemOption.onRemoveItem(dragItem)
                    infoItemIdentifier -> itemOption.onInfoItem(dragItem)
                    resizeItemIdentifier -> itemOption.onResizeItem(dragItem)
                    startShortcutItemIdentifier -> itemOption.onStartShortcutItem(dragItem, position)
                }
            }
            collapse()
            true
        }
    }

    fun showItemPopupForLockedDesktop(item: Item, homeActivity: HomeActivity) {
        val itemList = ArrayList<AbstractPopupIconLabelItem>()
        when (item.type) {
            Item.Type.APP -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 && item.shortcutInfo != null) {
                    item.shortcutInfo?.forEach { shortcutInfo ->
                        itemList.add(getAppShortcutItem(shortcutInfo))
                    }
                }
            }
            else -> {}
        }

        var x = dragLocation.x - HomeActivity._itemTouchX + Tool.dp2px(10)
        var y = dragLocation.y - HomeActivity._itemTouchY - Tool.dp2px(46 * itemList.size)

        if (x + Tool.dp2px(200) > width) {
            setPopupMenuShowDirection(false)
            x = dragLocation.x - HomeActivity._itemTouchX +
                    homeActivity.desktop.currentPage.cellWidth - Tool.dp2px(200) - Tool.dp2px(10)
        } else {
            setPopupMenuShowDirection(true)
        }

        if (y < 0) {
            y = dragLocation.y - HomeActivity._itemTouchY +
                    homeActivity.desktop.currentPage.cellHeight + Tool.dp2px(4)
        } else {
            y -= Tool.dp2px(4)
        }

        showPopupMenuForItem(x.toFloat(), y.toFloat(), itemList) { v, adapter, item1, position ->
            val itemOption = HpItemOption(homeActivity)
            when (item1.identifier.toInt()) {
                uninstallItemIdentifier -> itemOption.onUninstallItem(item)
                infoItemIdentifier -> itemOption.onInfoItem(item)
                startShortcutItemIdentifier -> itemOption.onStartShortcutItem(item, position)
            }
            collapse()
            true
        }
    }

    private fun getAppShortcutItem(shortcutInfo: ShortcutInfo): PopupDynamicIconLabelItem {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            PopupDynamicIconLabelItem(
                shortcutInfo.shortLabel,
                launcherApps.getShortcutIconDrawable(shortcutInfo, context.resources.displayMetrics.densityDpi)
            ).withIdentifier(startShortcutItemIdentifier.toLong())
        } else {
            throw IllegalStateException("getAppShortcutItem should not be called below N_MR1")
        }
    }

    private fun handleMovement() {
        if (!_dragExceedThreshold &&
            (Math.abs(_dragLocationStart.x - _dragLocation.x) > DRAG_THRESHOLD ||
                    Math.abs(_dragLocationStart.y - _dragLocation.y) > DRAG_THRESHOLD)
        ) {
            _dragExceedThreshold = true
            for ((dropTargetListener, dragFlag) in _registeredDropTargetEntries) {
                if (!dragFlag.shouldIgnore) {
                    convertPoint(dropTargetListener.view)
                    dropTargetListener.onStartDrag(_dragAction, _dragLocationConverted)
                }
            }
        }
        if (_dragExceedThreshold) {
            collapse()
        }
        for ((dropTargetListener, dragFlag) in _registeredDropTargetEntries) {
            if (!dragFlag.shouldIgnore) {
                convertPoint(dropTargetListener.view)
                if (isViewContains(dropTargetListener.view, _dragLocation.x.toInt(), _dragLocation.y.toInt())) {
                    dropTargetListener.onMove(_dragAction, _dragLocationConverted)
                    if (dragFlag.previousOutside) {
                        dragFlag.previousOutside = false
                        dropTargetListener.onEnter(_dragAction, _dragLocationConverted)
                    }
                } else if (!dragFlag.previousOutside) {
                    dragFlag.previousOutside = true
                    dropTargetListener.onExit(_dragAction, _dragLocationConverted)
                }
            }
        }
    }

    private fun handleDragFinished() {
        _dragging = false
        for ((dropTargetListener, dragFlag) in _registeredDropTargetEntries) {
            if (!dragFlag.shouldIgnore) {
                if (isViewContains(dropTargetListener.view, _dragLocation.x.toInt(), _dragLocation.y.toInt())) {
                    convertPoint(dropTargetListener.view)
                    dropTargetListener.onDrop(_dragAction, _dragLocationConverted, _dragItem)
                }
            }
        }
        for ((dropTargetListener, _) in _registeredDropTargetEntries) {
            dropTargetListener.onEnd()
        }
        cancelFolderPreview()
    }

    fun convertPoint(toView: View) {
        val fromCoordinate = IntArray(2)
        val toCoordinate = IntArray(2)
        getLocationOnScreen(fromCoordinate)
        toView.getLocationOnScreen(toCoordinate)
        _dragLocationConverted.set(
            (fromCoordinate[0] - toCoordinate[0]).toFloat() + _dragLocation.x,
            (fromCoordinate[1] - toCoordinate[1]).toFloat() + _dragLocation.y
        )
    }

    private fun isViewContains(view: View, rx: Int, ry: Int): Boolean {
        view.getLocationOnScreen(_tempArrayOfInt2)
        val x = _tempArrayOfInt2[0]
        val y = _tempArrayOfInt2[1]
        val w = view.width
        val h = view.height

        return !(rx < x || rx > x + w || ry < y || ry > y + h)
    }
}
