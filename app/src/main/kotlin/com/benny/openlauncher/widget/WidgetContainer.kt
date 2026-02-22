package com.benny.openlauncher.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.graphics.Point
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import com.benny.openlauncher.R
import com.benny.openlauncher.activity.HomeActivity
import com.benny.openlauncher.model.Item
import kotlin.math.max
import kotlin.math.min

class WidgetContainer(context: Context, widgetView: WidgetView, private val item: Item) : FrameLayout(context) {

    private val ve: View
    private val he: View
    private val vl: View
    private val hl: View

    private val action = Runnable {
        ve.animate().scaleY(0f).scaleX(0f)
        he.animate().scaleY(0f).scaleX(0f)
        vl.animate().scaleY(0f).scaleX(0f)
        hl.animate().scaleY(0f).scaleX(0f)
    }

    init {
        addView(widgetView)
        val layoutInflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        layoutInflater.inflate(R.layout.view_widget_container, this)

        ve = findViewById(R.id.vertexpand)
        he = findViewById(R.id.horiexpand)
        vl = findViewById(R.id.vertless)
        hl = findViewById(R.id.horiless)

        ve.setOnClickListener { view ->
            if (view.scaleX < 1) return@setOnClickListener
            item.spanY = item.spanY + 1
            scaleWidget(this, item)
            removeCallbacks(action)
            postDelayed(action, 2000)
        }

        he.setOnClickListener { view ->
            if (view.scaleX < 1) return@setOnClickListener
            item.spanX = item.spanX + 1
            scaleWidget(this, item)
            removeCallbacks(action)
            postDelayed(action, 2000)
        }

        vl.setOnClickListener { view ->
            if (view.scaleX < 1) return@setOnClickListener
            item.spanY = item.spanY - 1
            scaleWidget(this, item)
            removeCallbacks(action)
            postDelayed(action, 2000)
        }

        hl.setOnClickListener { view ->
            if (view.scaleX < 1) return@setOnClickListener
            item.spanX = item.spanX - 1
            scaleWidget(this, item)
            removeCallbacks(action)
            postDelayed(action, 2000)
        }
    }

    fun showResize() {
        ve.animate().scaleY(1f).scaleX(1f)
        he.animate().scaleY(1f).scaleX(1f)
        vl.animate().scaleY(1f).scaleX(1f)
        hl.animate().scaleY(1f).scaleX(1f)

        postDelayed(action, 2000)
    }

    fun scaleWidget(view: View, item: Item) {
        val launcher = HomeActivity.launcher ?: return
        val currentPage = launcher.desktop.currentPage

        item.spanX = min(item.spanX, currentPage.cellSpanH)
        item.spanX = max(item.spanX, 1)
        item.spanY = min(item.spanY, currentPage.cellSpanV)
        item.spanY = max(item.spanY, 1)

        currentPage.setOccupied(false, view.layoutParams as CellContainer.LayoutParams)

        if (!currentPage.checkOccupied(Point(item.x, item.y), item.spanX, item.spanY)) {
            val newWidgetLayoutParams = CellContainer.LayoutParams(
                CellContainer.LayoutParams.WRAP_CONTENT,
                CellContainer.LayoutParams.WRAP_CONTENT,
                item.x,
                item.y,
                item.spanX,
                item.spanY
            )

            // update occupied array
            currentPage.setOccupied(true, newWidgetLayoutParams)

            // update the view
            view.layoutParams = newWidgetLayoutParams
            updateWidgetOption(item)

            // update the widget size in the database
            HomeActivity._db.saveItem(item)
        } else {
            Toast.makeText(
                launcher.desktop.context,
                R.string.toast_not_enough_space,
                Toast.LENGTH_SHORT
            ).show()

            // add the old layout params to the occupied array
            currentPage.setOccupied(true, view.layoutParams as CellContainer.LayoutParams)
        }
    }

    fun updateWidgetOption(item: Item) {
        val launcher = HomeActivity.launcher ?: return
        val currentPage = launcher.desktop.currentPage
        val cellWidth = currentPage.cellWidth
        val cellHeight = currentPage.cellHeight

        if (cellWidth < 1 || cellHeight < 1) {
            // desktop isn't laid out
            return
        }

        val newOps = Bundle().apply {
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, item.spanX * cellWidth)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, item.spanX * cellWidth)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, item.spanY * cellHeight)
            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, item.spanY * cellHeight)
        }
        HomeActivity._appWidgetManager.updateAppWidgetOptions(item.widgetValue, newOps)
    }
}
