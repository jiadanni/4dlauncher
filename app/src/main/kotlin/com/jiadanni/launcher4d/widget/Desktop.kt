package com.jiadanni.launcher4d.widget

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Point
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.jiadanni.launcher4d.activity.HomeActivity
import com.jiadanni.launcher4d.manager.Setup
import com.jiadanni.launcher4d.model.Item
import com.jiadanni.launcher4d.model.Item.Type
import com.jiadanni.launcher4d.util.Definitions
import com.jiadanni.launcher4d.util.Definitions.ItemPosition
import com.jiadanni.launcher4d.util.Definitions.ItemState
import com.jiadanni.launcher4d.util.Definitions.WallpaperScroll
import com.jiadanni.launcher4d.util.DragAction.Action
import com.jiadanni.launcher4d.util.DragHandler
import com.jiadanni.launcher4d.util.LauncherAction
import com.jiadanni.launcher4d.util.Tool
import com.jiadanni.launcher4d.viewutil.DesktopCallback
import com.jiadanni.launcher4d.viewutil.DesktopGestureListener
import com.jiadanni.launcher4d.viewutil.ItemViewFactory
import com.jiadanni.launcher4d.viewutil.MultiTouchGestureDetector
import com.jiadanni.launcher4d.widget.CellContainer.DragState
import in.championswimmer.sfg.lib.SimpleFingerGestures

class Desktop : ViewPager, DesktopCallback {
    private var _desktopEditListener: OnDesktopEditListener? = null
    private var _inEditMode: Boolean = false
    private var _pageIndicator: PagerIndicator? = null

    private val _pages: MutableList<CellContainer> = mutableListOf()
    private val _previousDragPoint = Point()

    private var _coordinate = Point(-1, -1)
    private lateinit var _adapter: DesktopAdapter
    private var _previousItem: Item? = null
    private var _previousItemView: View? = null
    private var _previousPage: Int = 0

    // For feed swipe gesture detection
    private var _feedSwipeStartX: Float = 0f
    private var _feedSwipeStartY: Float = 0f
    private var _isFeedSwipeDetected: Boolean = false

    // Multi-touch gesture detector
    private lateinit var _multiTouchGestureDetector: MultiTouchGestureDetector

    constructor(context: Context) : super(context, null) {
        init()
    }

    constructor(context: Context, attr: AttributeSet?) : super(context, attr) {
        init()
    }

    private fun init() {
        // Initialize multi-touch gesture detector
        _multiTouchGestureDetector = MultiTouchGestureDetector(
            context,
            MultiTouchGestureDetector.createListener(
                onPinchIn = { handleGesture(com.jiadanni.launcher4d.R.string.pref_key__gesture_pinch_in) },
                onPinchOut = { handleGesture(com.jiadanni.launcher4d.R.string.pref_key__gesture_pinch_out) },
                onTwoFingerScrollUp = { handleGesture(com.jiadanni.launcher4d.R.string.pref_key__gesture_two_finger_scroll_up) },
                onTwoFingerScrollDown = { handleGesture(com.jiadanni.launcher4d.R.string.pref_key__gesture_two_finger_scroll_down) },
                onTwoFingerDoubleTap = { handleGesture(com.jiadanni.launcher4d.R.string.pref_key__gesture_two_finger_double_tap) }
            )
        )
    }

    /**
     * Handle a multi-touch gesture by executing the configured action
     */
    private fun handleGesture(gestureKey: Int): Boolean {
        val gesture = Setup.appSettings().getGesture(gestureKey)

        if (Setup.appSettings().getGestureFeedback()) {
            Tool.vibrate(this)
        }

        return when (gesture) {
            is android.content.Intent -> {
                // Launch app
                Tool.startApp(context, gesture, null)
                true
            }
            is LauncherAction.ActionDisplayItem -> {
                // Execute launcher action
                LauncherAction.RunAction(gesture._action, context)
                true
            }
            else -> false
        }
    }

    inner class DesktopAdapter(private val _desktop: Desktop) : PagerAdapter() {
        init {
            _desktop.pages.clear()
            var count = Setup.dataManager().getDesktop().size
            if (count == 0) count++
            for (i in 0 until count) {
                _desktop.pages.add(getItemLayout())
            }
        }

        private fun getGestureListener(): SimpleFingerGestures.OnFingerGestureListener {
            return DesktopGestureListener(_desktop, Setup.desktopGestureCallback())
        }

        private fun getItemLayout(): CellContainer {
            val context = _desktop.context
            val layout = CellContainer(context)
            val mySfg = SimpleFingerGestures()
            mySfg.setOnFingerGestureListener(getGestureListener())
            layout.setGestures(mySfg)
            layout.setGridSize(Setup.appSettings().getDesktopColumnCount(), Setup.appSettings().getDesktopRowCount())
            layout.setOnClickListener {
                exitDesktopEditMode()
            }
            layout.setOnLongClickListener {
                enterDesktopEditMode()
                if (Setup.appSettings().getGestureFeedback()) {
                    Tool.vibrate(_desktop)
                }
                true
            }
            return layout
        }

        fun addPageLeft() {
            // Shift pages to the right (including home page)
            Setup.dataManager().addPage(0)
            Setup.appSettings().setDesktopPageCurrent(Setup.appSettings().getDesktopPageCurrent() + 1)

            _desktop.pages.add(0, getItemLayout())
            notifyDataSetChanged()
        }

        fun addPageRight() {
            _desktop.pages.add(getItemLayout())
            notifyDataSetChanged()
        }

        fun removePage(position: Int, deleteItems: Boolean) {
            if (deleteItems) {
                for (view in _desktop.pages[position].getAllCells()) {
                    val item = view.tag
                    if (item is Item) {
                        Setup.dataManager().deleteItem(item, true)
                    }
                }
            }

            // Shift pages to the left (including home page)
            Setup.dataManager().removePage(position)
            if (Setup.appSettings().getDesktopPageCurrent() > position) {
                Setup.appSettings().setDesktopPageCurrent(Setup.appSettings().getDesktopPageCurrent() - 1)
            }

            _desktop.pages.remove(position)
            notifyDataSetChanged()
        }

        override fun getItemPosition(obj: Any): Int {
            return POSITION_NONE
        }

        override fun getCount(): Int {
            return _desktop.pages.size
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return super.getPageTitle(position)
        }

        override fun isViewFromObject(p1: View, p2: Any): Boolean {
            return p1 === p2
        }

        override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
            container.removeView(obj as View)
        }

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val layout = _desktop.pages[position]
            container.addView(layout)
            return layout
        }

        private fun enterDesktopEditMode() {
            val scaleFactor = 0.8f
            val translateFactor = Tool.dp2px(if (Setup.appSettings().getSearchBarEnable()) 20 else 40).toFloat()
            for (v in _desktop.pages) {
                v.setBlockTouch(true)
                v.animateBackgroundShow()
                val animation = v.animate().scaleX(scaleFactor).scaleY(scaleFactor).translationY(translateFactor)
                animation.interpolator = AccelerateDecelerateInterpolator()
            }
            _desktop.setInEditMode(true)
            _desktop.desktopEditListener?.onStartDesktopEdit()
        }

        private fun exitDesktopEditMode() {
            val scaleFactor = 1.0f
            val translateFactor = 0.0f
            for (v in _desktop.pages) {
                v.setBlockTouch(false)
                v.animateBackgroundHide()
                val animation = v.animate().scaleX(scaleFactor).scaleY(scaleFactor).translationY(translateFactor)
                animation.interpolator = AccelerateDecelerateInterpolator()
            }
            _desktop.setInEditMode(false)
            _desktop.desktopEditListener?.onFinishDesktopEdit()
        }
    }

    val pages: MutableList<CellContainer>
        get() = _pages

    var desktopEditListener: OnDesktopEditListener?
        get() = _desktopEditListener
        set(v) {
            _desktopEditListener = v
        }

    var inEditMode: Boolean
        get() = _inEditMode
        set(v) {
            _inEditMode = v
        }

    val isCurrentPageEmpty: Boolean
        get() = currentPage.childCount == 0

    val currentPage: CellContainer
        get() = _pages[currentItem]

    fun setPageIndicator(pageIndicator: PagerIndicator?) {
        _pageIndicator = pageIndicator
    }

    fun initDesktop() {
        _adapter = DesktopAdapter(this)
        adapter = _adapter
        setCurrentItem(Setup.appSettings().getDesktopPageCurrent())

        if (Setup.appSettings().getDesktopShowIndicator() && _pageIndicator != null) {
            _pageIndicator?.setViewPager(this)
        }
        addItemsToPage()
    }

    private fun addItemsToPage() {
        val columns = Setup.appSettings().getDesktopColumnCount()
        val rows = Setup.appSettings().getDesktopRowCount()
        val desktopItems = Setup.dataManager().getDesktop()
        for (pageCount in desktopItems.indices) {
            val page = desktopItems[pageCount]
            _pages[pageCount].removeAllViews()
            for (itemCount in page.indices) {
                val item = page[itemCount]
                if (item._x + item._spanX <= columns && item._y + item._spanY <= rows) {
                    addItemToPage(item, pageCount)
                }
            }
        }
    }

    fun updateDesktop() {
        addItemsToPage()
    }

    fun addPageRight(showGrid: Boolean) {
        val previousPage = currentItem
        _adapter.addPageRight()
        setCurrentItem(previousPage + 1)
        if (Setup.appSettings().getDesktopShowGrid()) {
            for (cellContainer in _pages) {
                cellContainer.setHideGrid(!showGrid)
            }
        }
        _pageIndicator?.invalidate()
    }

    fun addPageLeft(showGrid: Boolean) {
        val previousPage = currentItem
        _adapter.addPageLeft()
        setCurrentItem(previousPage + 1, false)
        setCurrentItem(previousPage)
        if (Setup.appSettings().getDesktopShowGrid()) {
            for (cellContainer in _pages) {
                cellContainer.setHideGrid(!showGrid)
            }
        }
        _pageIndicator?.invalidate()
    }

    fun removeCurrentPage() {
        val previousPage = currentItem
        _adapter.removePage(currentItem, true)
        if (_pages.size == 0) {
            addPageRight(false)
            _adapter.exitDesktopEditMode()
        } else {
            setCurrentItem(previousPage, true)
            _pageIndicator?.invalidate()
        }
    }

    fun updateIconProjection(x: Int, y: Int) {
        val launcher = Tool.getLauncher(context)
        val dragNDropView = launcher?.itemOptionView
        val state = currentPage.peekItemAndSwap(x, y, _coordinate)
        if (_coordinate != _previousDragPoint) {
            dragNDropView?.cancelFolderPreview()
        }
        _previousDragPoint.set(_coordinate.x, _coordinate.y)
        when (state) {
            DragState.CurrentNotOccupied -> {
                currentPage.projectImageOutlineAt(_coordinate, DragHandler._cachedDragBitmap)
            }
            DragState.CurrentOccupied -> {
                val type = dragNDropView?.getDragItem()?._type
                for (page in _pages) {
                    page.clearCachedOutlineBitmap()
                }
                if (type != Type.WIDGET && currentPage.coordinateToChildView(_coordinate) is AppItemView) {
                    dragNDropView?.showFolderPreviewAt(
                        this,
                        currentPage.getCellWidth() * (_coordinate.x + 0.5f),
                        currentPage.getCellHeight() * (_coordinate.y + 0.5f)
                    )
                }
            }
            DragState.OutOffRange,
            DragState.ItemViewNotFound -> {
                // Do nothing
            }
            else -> {
                // Do nothing
            }
        }
    }

    override fun setLastItem(item: Item?, view: View?) {
        _previousPage = currentItem
        _previousItemView = view
        _previousItem = item
        view?.let { currentPage.removeView(it) }
    }

    override fun revertLastItem() {
        _previousItemView?.let { view ->
            if (_adapter.count >= _previousPage && _previousPage > -1) {
                val cellContainer = _pages[_previousPage]
                cellContainer.addViewToGrid(view)
                _previousItem = null
                _previousItemView = null
                _previousPage = -1
            }
        }
    }

    override fun consumeLastItem() {
        _previousItem = null
        _previousItemView = null
        _previousPage = -1
    }

    fun addItemToPage(item: Item, page: Int): Boolean {
        val itemView = ItemViewFactory.getItemView(context, this, Action.DESKTOP, item)
        if (itemView == null) {
            // TODO see if this fixes SD card bug
            // apps that are located on SD card disappear on reboot
            // might be from this line of code so comment out for now
            //Setup.dataManager().deleteItem(item, true)
            return false
        }
        item._location = ItemPosition.Desktop
        _pages[page].addViewToGrid(itemView, item._x, item._y, item._spanX, item._spanY)
        return true
    }

    fun addItemToPoint(item: Item, x: Int, y: Int): Boolean {
        val positionToLayoutPrams = currentPage.coordinateToLayoutParams(x, y, item._spanX, item._spanY)
            ?: return false
        item._location = ItemPosition.Desktop
        item._x = positionToLayoutPrams.getX()
        item._y = positionToLayoutPrams.getY()
        val itemView = ItemViewFactory.getItemView(context, this, Action.DESKTOP, item)
        itemView?.let {
            it.layoutParams = positionToLayoutPrams
            currentPage.addView(it)
        }
        return true
    }

    fun addItemToCell(item: Item, x: Int, y: Int): Boolean {
        item._location = ItemPosition.Desktop
        item._x = x
        item._y = y
        val itemView = ItemViewFactory.getItemView(context, this, Action.DESKTOP, item)
            ?: return false
        currentPage.addViewToGrid(itemView, item._x, item._y, item._spanX, item._spanY)
        return true
    }

    fun removeItem(view: View, animate: Boolean) {
        if (animate) {
            view.animate().setDuration(100).scaleX(0.0f).scaleY(0.0f).withEndAction {
                if (currentPage == view.parent) {
                    currentPage.removeView(view)
                }
            }
        } else if (currentPage == view.parent) {
            currentPage.removeView(view)
        }
    }

    override fun onPageScrolled(position: Int, offset: Float, offsetPixels: Int) {
        val scroll = Setup.appSettings().getDesktopWallpaperScroll()
        var xOffset = (position + offset) / (_pages.size - 1)
        when (scroll) {
            WallpaperScroll.Inverse -> xOffset = 1f - xOffset
            WallpaperScroll.Off -> xOffset = 0.5f
            WallpaperScroll.Normal -> {
                // Keep xOffset as is
            }
        }

        val wallpaperManager = WallpaperManager.getInstance(context)
        wallpaperManager.setWallpaperOffsets(windowToken, xOffset, 0.0f)
        super.onPageScrolled(position, offset, offsetPixels)
    }

    override fun onTouchEvent(ev: android.view.MotionEvent): Boolean {
        // First check multi-touch gestures
        if (_multiTouchGestureDetector.onTouchEvent(ev)) {
            return true
        }

        // Detect swipe-right gesture from page 0 to open feed
        when (ev.action) {
            android.view.MotionEvent.ACTION_DOWN -> {
                _feedSwipeStartX = ev.x
                _feedSwipeStartY = ev.y
                _isFeedSwipeDetected = false
            }
            android.view.MotionEvent.ACTION_MOVE -> {
                // Only detect if we're on the first page
                if (currentItem == 0 && !_isFeedSwipeDetected) {
                    val deltaX = ev.x - _feedSwipeStartX
                    val deltaY = ev.y - _feedSwipeStartY

                    // Swipe right (positive deltaX) from page 0
                    // Require horizontal swipe to be dominant over vertical
                    if (deltaX > 150 && Math.abs(deltaY) < Math.abs(deltaX) / 2) {
                        _isFeedSwipeDetected = true
                        Tool.getLauncher(context)?.openFeed()
                        return true
                    }
                }
            }
            android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                _isFeedSwipeDetected = false
            }
        }

        return super.onTouchEvent(ev)
    }

    interface OnDesktopEditListener {
        fun onStartDesktopEdit()
        fun onFinishDesktopEdit()
    }

    companion object {
        @JvmStatic
        fun handleOnDropOver(
            homeActivity: HomeActivity,
            dropItem: Item?,
            item: Item?,
            itemView: View?,
            parent: CellContainer,
            page: Int,
            itemPosition: ItemPosition,
            callback: DesktopCallback
        ): Boolean {
            if (item == null) return false
            if (dropItem == null) return false

            val type = item._type ?: return false

            when (type) {
                Type.APP, Type.SHORTCUT -> {
                    if (Type.APP == dropItem._type || Type.SHORTCUT == dropItem._type) {
                        itemView?.let { parent.removeView(it) }
                        val group = Item.newGroupItem()
                        item._location = ItemPosition.Group
                        dropItem._location = ItemPosition.Group
                        group.getGroupItems().add(item)
                        group.getGroupItems().add(dropItem)
                        group._x = item._x
                        group._y = item._y
                        Setup.dataManager().saveItem(dropItem, page, ItemPosition.Group)
                        Setup.dataManager().saveItem(item, ItemState.Hidden)
                        Setup.dataManager().saveItem(dropItem, ItemState.Hidden)
                        Setup.dataManager().saveItem(group, page, itemPosition)
                        callback.addItemToPage(group, page)
                        homeActivity.desktop.consumeLastItem()
                        homeActivity.dock.consumeLastItem()
                        return true
                    } else if (Type.GROUP == dropItem._type && dropItem.getGroupItems().size < GroupPopupView.GroupDef._maxItem) {
                        itemView?.let { parent.removeView(it) }
                        val group = Item.newGroupItem()
                        item._location = ItemPosition.Group
                        dropItem._location = ItemPosition.Group
                        group.getGroupItems().add(item)
                        group.getGroupItems().addAll(dropItem.getGroupItems())
                        group._x = item._x
                        group._y = item._y
                        Setup.dataManager().deleteItem(dropItem, false)
                        Setup.dataManager().saveItem(item, ItemState.Hidden)
                        Setup.dataManager().saveItem(group, page, itemPosition)
                        callback.addItemToPage(group, page)
                        homeActivity.desktop.consumeLastItem()
                        homeActivity.dock.consumeLastItem()
                        return true
                    }
                }
                Type.GROUP -> {
                    if ((Type.APP == dropItem._type || Type.SHORTCUT == dropItem._type) && item.getGroupItems().size < GroupPopupView.GroupDef._maxItem) {
                        itemView?.let { parent.removeView(it) }
                        dropItem._location = ItemPosition.Group
                        item.getGroupItems().add(dropItem)
                        Setup.dataManager().saveItem(dropItem, page, ItemPosition.Group)
                        Setup.dataManager().saveItem(dropItem, ItemState.Hidden)
                        Setup.dataManager().saveItem(item, page, itemPosition)
                        callback.addItemToPage(item, page)
                        homeActivity.desktop.consumeLastItem()
                        homeActivity.dock.consumeLastItem()
                        return true
                    } else if (Type.GROUP == dropItem._type && item.getGroupItems().size < GroupPopupView.GroupDef._maxItem && dropItem.getGroupItems().size < GroupPopupView.GroupDef._maxItem) {
                        itemView?.let { parent.removeView(it) }
                        item.getGroupItems().addAll(dropItem.getGroupItems())
                        Setup.dataManager().saveItem(item, page, itemPosition)
                        Setup.dataManager().deleteItem(dropItem, false)
                        callback.addItemToPage(item, page)
                        homeActivity.desktop.consumeLastItem()
                        homeActivity.dock.consumeLastItem()
                        return true
                    }
                }
                else -> {
                    // Do nothing
                }
            }
            return false
        }
    }
}
