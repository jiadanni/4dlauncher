package com.jiadanni.launcher4d.widget

import android.content.Context
import android.content.res.Configuration
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.jiadanni.launcher4d.R
import com.jiadanni.launcher4d.interfaces.AppUpdateListener
import com.jiadanni.launcher4d.manager.Setup
import com.jiadanni.launcher4d.model.App
import com.jiadanni.launcher4d.util.Tool
import com.jiadanni.launcher4d.viewutil.IconLabelItem
import com.mikepenz.fastadapter.FastAdapter
import com.mikepenz.fastadapter.adapters.ItemAdapter
import com.turingtechnologies.materialscrollbar.AlphabetIndicator
import com.turingtechnologies.materialscrollbar.DragScrollBar
import com.turingtechnologies.materialscrollbar.INameableAdapter

class AppDrawerGrid(context: Context) : FrameLayout(context) {

    lateinit var recyclerView: RecyclerView
    lateinit var gridDrawerAdapter: AppDrawerGridAdapter
    lateinit var scrollBar: DragScrollBar

    private lateinit var layoutManager: GridLayoutManager

    init {
        val layoutInflater = LayoutInflater.from(getContext())
        val view = layoutInflater.inflate(R.layout.view_app_drawer_grid, this@AppDrawerGrid, false)
        addView(view)

        recyclerView = findViewById(R.id.recycler_view)
        scrollBar = findViewById(R.id.scroll_bar)
        layoutManager = GridLayoutManager(getContext(), Setup.appSettings().drawerColumnCount)

        init()
    }

    private fun init() {
        if (!Setup.appSettings().drawerShowIndicator) {
            scrollBar.visibility = GONE
        }
        scrollBar.setIndicator(AlphabetIndicator(context), true)
        scrollBar.setClipToPadding(true)
        scrollBar.setDraggableFromAnywhere(true)
        scrollBar.setHandleColor(Setup.appSettings().drawerFastScrollColor)

        gridDrawerAdapter = AppDrawerGridAdapter()

        if (context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            setPortraitValue()
        } else {
            setLandscapeValue()
        }
        recyclerView.adapter = gridDrawerAdapter
        recyclerView.layoutManager = layoutManager
        recyclerView.setDrawingCacheEnabled(true)

        viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                _itemWidth = width / layoutManager.spanCount
                _itemHeightPadding = Tool.dp2px(20)
                updateAdapter(Setup.appLoader().getAllApps(getContext(), false))
                Setup.appLoader().addUpdateListener(object : AppUpdateListener {
                    override fun onAppUpdated(apps: List<App>): Boolean {
                        updateAdapter(apps)
                        return false
                    }
                })
            }
        })
    }

    fun updateAdapter(apps: List<App>) {
        _apps = apps
        val items = ArrayList<IconLabelItem>()
        for (app in apps) {
            items.add(IconLabelItem(app))
        }
        gridDrawerAdapter.itemAdapter.set(items)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        if (_apps == null) {
            super.onConfigurationChanged(newConfig)
            return
        }

        when (newConfig.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> setLandscapeValue()
            Configuration.ORIENTATION_PORTRAIT -> setPortraitValue()
        }
        super.onConfigurationChanged(newConfig)
    }

    private fun setPortraitValue() {
        layoutManager.spanCount = Setup.appSettings().drawerColumnCount
        gridDrawerAdapter.notifyAdapterDataSetChanged()
    }

    private fun setLandscapeValue() {
        layoutManager.spanCount = Setup.appSettings().drawerRowCount
        gridDrawerAdapter.notifyAdapterDataSetChanged()
    }

    class AppDrawerGridAdapter : FastAdapter<IconLabelItem>(), INameableAdapter {
        val itemAdapter = ItemAdapter<IconLabelItem>()
        init { addAdapter(0, itemAdapter) }

        override fun getCharacterForElement(element: Int): Char {
            return if (_apps != null && element < _apps!!.size && _apps!![element].label.isNotEmpty()) {
                _apps!![element].label[0]
            } else {
                '#'
            }
        }
    }

    companion object {
        @JvmStatic
        var _itemWidth: Int = 0

        @JvmStatic
        var _itemHeightPadding: Int = 0

        private var _apps: List<App>? = null
    }
}
