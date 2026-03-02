package com.jiadanni.launcher4d.widget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.jiadanni.launcher4d.R
import com.jiadanni.launcher4d.interfaces.AppUpdateListener
import com.jiadanni.launcher4d.manager.Setup
import com.jiadanni.launcher4d.model.App
import com.jiadanni.launcher4d.model.Item
import com.jiadanni.launcher4d.util.DragAction
import com.jiadanni.launcher4d.util.Tool
import com.jiadanni.launcher4d.viewutil.ItemViewFactory

class AppDrawerPage @JvmOverloads constructor(
    context: Context,
    attr: AttributeSet? = null
) : ViewPager(context, attr) {

    private var apps: List<App>? = null
    val pages = ArrayList<ViewGroup>()

    private var appDrawerIndicator: PagerIndicator? = null
    private var pageCount = 0

    init {
        init(context)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        if (apps == null) {
            super.onConfigurationChanged(newConfig)
            return
        }

        when (newConfig.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                setLandscapeValue()
                calculatePage()
                adapter = Adapter()
            }
            Configuration.ORIENTATION_PORTRAIT -> {
                setPortraitValue()
                calculatePage()
                adapter = Adapter()
            }
        }
        super.onConfigurationChanged(newConfig)
    }

    private fun setPortraitValue() {
        _columnCellCount = Setup.appSettings().drawerColumnCount
        _rowCellCount = Setup.appSettings().drawerRowCount
    }

    private fun setLandscapeValue() {
        _columnCellCount = Setup.appSettings().drawerRowCount
        _rowCellCount = Setup.appSettings().drawerColumnCount
    }

    private fun calculatePage() {
        pageCount = 0
        var appsSize = apps?.size ?: 0
        while (appsSize.also { appsSize = it - (_rowCellCount * _columnCellCount) } >= (_rowCellCount * _columnCellCount) || appsSize > -(_rowCellCount * _columnCellCount)) {
            pageCount++
        }
    }

    private fun init(c: Context) {
        if (isInEditMode) return

        overScrollMode = OVER_SCROLL_NEVER

        val mPortrait = c.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT

        if (mPortrait) {
            setPortraitValue()
        } else {
            setLandscapeValue()
        }

        val allApps = Setup.appLoader().getAllApps(c, false)
        if (allApps.isNotEmpty()) {
            this@AppDrawerPage.apps = allApps
            calculatePage()
            adapter = Adapter()
            if (appDrawerIndicator != null && Setup.appSettings().drawerShowIndicator) {
                appDrawerIndicator?.setViewPager(this@AppDrawerPage)
            }
        }

        Setup.appLoader().addUpdateListener(object : AppUpdateListener {
            override fun onAppUpdated(apps: List<App>): Boolean {
                this@AppDrawerPage.apps = apps
                calculatePage()
                adapter = Adapter()
                if (appDrawerIndicator != null && Setup.appSettings().drawerShowIndicator) {
                    appDrawerIndicator?.setViewPager(this@AppDrawerPage)
                }
                return false
            }
        })
    }

    fun withHome(appDrawerIndicator: PagerIndicator) {
        this.appDrawerIndicator = appDrawerIndicator
        appDrawerIndicator.setMode(PagerIndicator.Mode.DOTS)
        if (adapter != null && Setup.appSettings().drawerShowIndicator) {
            appDrawerIndicator.setViewPager(this@AppDrawerPage)
        }
    }

    inner class Adapter : PagerAdapter() {

        init {
            pages.clear()
            for (i in 0 until count) {
                val layout = LayoutInflater.from(context).inflate(
                    R.layout.view_app_drawer_page_inner, null
                ) as ViewGroup

                val cardView = layout.getChildAt(0) as CardView
                if (!Setup.appSettings().drawerShowCardView) {
                    cardView.setCardBackgroundColor(Color.TRANSPARENT)
                    cardView.cardElevation = 0f
                } else {
                    cardView.setCardBackgroundColor(Setup.appSettings().drawerCardColor)
                    cardView.cardElevation = Tool.dp2px(4)
                }

                val cc = layout.findViewById<CellContainer>(R.id.group)
                cc.setGridSize(_columnCellCount, _rowCellCount)

                for (x in 0 until _columnCellCount) {
                    for (y in 0 until _rowCellCount) {
                        val view = getItemView(i, x, y)
                        if (view != null) {
                            val lp = CellContainer.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                x, y, 1, 1
                            )
                            view.layoutParams = lp
                            cc.addViewToGrid(view)
                        }
                    }
                }
                pages.add(layout)
            }
        }

        private fun getItemView(page: Int, x: Int, y: Int): View? {
            val pagePos = y * _columnCellCount + x
            val pos = _rowCellCount * _columnCellCount * page + pagePos

            val currentApps = apps ?: return null
            if (pos >= currentApps.size) return null

            val app = currentApps[pos]

            return ItemViewFactory.getItemView(context, null, DragAction.Action.DRAWER, Item.newAppItem(app))
        }

        override fun getCount(): Int = pageCount

        override fun isViewFromObject(p1: View, p2: Any): Boolean = p1 == p2

        override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
            container.removeView(`object` as View)
        }

        override fun getItemPosition(`object`: Any): Int {
            val index = pages.indexOf(`object`)
            return if (index == -1) POSITION_NONE else index
        }

        override fun instantiateItem(container: ViewGroup, pos: Int): Any {
            val layout = pages[pos]
            container.addView(layout)
            return layout
        }
    }

    companion object {
        @JvmStatic
        var _columnCellCount: Int = 0

        @JvmStatic
        var _rowCellCount: Int = 0
    }
}
