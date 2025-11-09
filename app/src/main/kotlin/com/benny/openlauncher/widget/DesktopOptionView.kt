package com.benny.openlauncher.widget

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.benny.openlauncher.R
import com.benny.openlauncher.manager.Setup
import com.benny.openlauncher.util.Tool
import com.benny.openlauncher.viewutil.IconLabelItem
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.IAdapter
import com.mikepenz.fastadapter.commons.adapters.FastItemAdapter

class DesktopOptionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val actionRecyclerViews = arrayOfNulls<RecyclerView>(2)
    private val actionAdapters = arrayOfNulls<FastItemAdapter<IconLabelItem>>(2)
    private var desktopOptionViewListener: DesktopOptionViewListener? = null

    init {
        init()
    }

    fun setDesktopOptionViewListener(desktopOptionViewListener: DesktopOptionViewListener) {
        this.desktopOptionViewListener = desktopOptionViewListener
    }

    fun updateHomeIcon(home: Boolean) {
        post {
            val icon = if (home) {
                context.resources.getDrawable(R.drawable.ic_star, null)
            } else {
                context.resources.getDrawable(R.drawable.ic_star_border, null)
            }
            actionAdapters[0]?.getAdapterItem(1)?._icon = icon
            actionAdapters[0]?.notifyAdapterItemChanged(1)
        }
    }

    fun updateLockIcon(lock: Boolean) {
        if (actionAdapters.isEmpty()) return
        if (actionAdapters[0]?.adapterItemCount == 0) return

        post {
            val icon = if (lock) {
                context.resources.getDrawable(R.drawable.ic_lock, null)
            } else {
                context.resources.getDrawable(R.drawable.ic_lock_open, null)
            }
            actionAdapters[0]?.getAdapterItem(2)?._icon = icon
            actionAdapters[0]?.notifyAdapterItemChanged(2)
        }
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            setPadding(0, insets.systemWindowInsetTop, 0, insets.systemWindowInsetBottom)
            return insets
        }
        return insets
    }

    private fun init() {
        if (isInEditMode) {
            return
        }

        val paddingHorizontal = Tool.dp2px(42)
        val typeface = Typeface.createFromAsset(context.assets, "RobotoCondensed-Regular.ttf")

        actionAdapters[0] = FastItemAdapter()
        actionAdapters[1] = FastItemAdapter()

        actionRecyclerViews[0] = createRecyclerView(actionAdapters[0]!!, Gravity.TOP or Gravity.CENTER_HORIZONTAL, paddingHorizontal)
        actionRecyclerViews[1] = createRecyclerView(actionAdapters[1]!!, Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, paddingHorizontal)

        val clickListener = com.mikepenz.fastadapter.listeners.OnClickListener<IconLabelItem> { v, adapter, item, position ->
            desktopOptionViewListener?.let { listener ->
                val id = item.identifier.toInt()
                when (id) {
                    R.string.home -> {
                        updateHomeIcon(true)
                        listener.onSetHomePage()
                        return@OnClickListener true
                    }
                    R.string.remove -> {
                        if (!Setup.appSettings().desktopLock) {
                            listener.onRemovePage()
                        } else {
                            Tool.toast(context, "Desktop is locked.")
                        }
                        return@OnClickListener true
                    }
                    R.string.widget -> {
                        if (!Setup.appSettings().desktopLock) {
                            listener.onPickWidget()
                        } else {
                            Tool.toast(context, "Desktop is locked.")
                        }
                        return@OnClickListener true
                    }
                    R.string.action -> {
                        if (!Setup.appSettings().desktopLock) {
                            listener.onPickAction()
                        } else {
                            Tool.toast(context, "Desktop is locked.")
                        }
                        return@OnClickListener true
                    }
                    R.string.lock -> {
                        Setup.appSettings().desktopLock = !Setup.appSettings().desktopLock
                        updateLockIcon(Setup.appSettings().desktopLock)
                        return@OnClickListener true
                    }
                    R.string.pref_title__settings -> {
                        listener.onLaunchSettings()
                        return@OnClickListener true
                    }
                    else -> return@OnClickListener false
                }
            } ?: false
        }

        viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                val itemWidth = (width - 2 * paddingHorizontal) / 3
                initItems(typeface, clickListener, itemWidth)
            }
        })
    }

    private fun initItems(
        typeface: Typeface,
        clickListener: com.mikepenz.fastadapter.listeners.OnClickListener<IconLabelItem>,
        itemWidth: Int
    ) {
        val itemsTop = ArrayList<IconLabelItem>()
        itemsTop.add(createItem(R.drawable.ic_delete, R.string.remove, typeface, itemWidth))
        itemsTop.add(createItem(R.drawable.ic_star, R.string.home, typeface, itemWidth))
        itemsTop.add(createItem(R.drawable.ic_lock, R.string.lock, typeface, itemWidth))
        actionAdapters[0]?.set(itemsTop)
        actionAdapters[0]?.withOnClickListener(clickListener)

        val itemsBottom = ArrayList<IconLabelItem>()
        itemsBottom.add(createItem(R.drawable.ic_dashboard, R.string.widget, typeface, itemWidth))
        itemsBottom.add(createItem(R.drawable.ic_launch, R.string.action, typeface, itemWidth))
        itemsBottom.add(createItem(R.drawable.ic_settings, R.string.pref_title__settings, typeface, itemWidth))
        actionAdapters[1]?.set(itemsBottom)
        actionAdapters[1]?.withOnClickListener(clickListener)

        val topMargin = Tool.dp2px(if (Setup.appSettings().searchBarEnable) 36 else 4)
        ((actionRecyclerViews[0]?.parent as? View)?.layoutParams as? MarginLayoutParams)?.topMargin = topMargin
    }

    private fun createRecyclerView(adapter: FastAdapter<IconLabelItem>, gravity: Int, paddingHorizontal: Int): RecyclerView {
        val actionRecyclerView = RecyclerView(context)
        val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        actionRecyclerView.clipToPadding = false
        actionRecyclerView.setPadding(paddingHorizontal, 0, paddingHorizontal, 0)
        actionRecyclerView.layoutManager = linearLayoutManager
        actionRecyclerView.adapter = adapter
        actionRecyclerView.overScrollMode = OVER_SCROLL_ALWAYS

        val actionRecyclerViewLP = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        actionRecyclerViewLP.gravity = gravity

        addView(actionRecyclerView, actionRecyclerViewLP)
        return actionRecyclerView
    }

    private fun createItem(icon: Int, label: Int, typeface: Typeface, width: Int): IconLabelItem {
        return IconLabelItem(context, icon, label)
            .withIdentifier(label.toLong())
            .withOnClickListener(null)
            .withTextColor(Color.WHITE)
            .withIconSize(36)
            .withIconColor(Color.WHITE)
            .withIconPadding(4)
            .withIconGravity(Gravity.TOP)
            .withWidth(width)
            .withTextGravity(Gravity.CENTER)
    }

    interface DesktopOptionViewListener {
        fun onRemovePage()
        fun onSetHomePage()
        fun onPickWidget()
        fun onPickAction()
        fun onLaunchSettings()
    }
}
