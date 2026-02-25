package com.benny.openlauncher.activity.homeparts

import android.appwidget.AppWidgetManager
import android.content.Intent
import com.benny.openlauncher.R
import com.benny.openlauncher.activity.HomeActivity
import com.benny.openlauncher.activity.HomeActivity.Companion.REQUEST_CREATE_APPWIDGET
import com.benny.openlauncher.activity.HomeActivity.Companion.REQUEST_PICK_APPWIDGET
import com.benny.openlauncher.interfaces.DialogListener
import com.benny.openlauncher.manager.Setup
import com.benny.openlauncher.model.Item
import com.benny.openlauncher.util.Definitions
import com.benny.openlauncher.util.Tool
import com.benny.openlauncher.viewutil.DialogHelper
import com.benny.openlauncher.widget.DesktopOptionView

class HpDesktopOption(
    private val homeActivity: HomeActivity
) : DesktopOptionView.DesktopOptionViewListener, DialogListener.OnActionDialogListener {

    fun onRemovePage() {
        if (homeActivity.desktop.isCurrentPageEmpty) {
            homeActivity.desktop.removeCurrentPage()
            return
        }
        DialogHelper.alertDialog(
            homeActivity,
            homeActivity.getString(R.string.remove),
            "This page is not empty. Those items will also be removed."
        ) { _, _ ->
            homeActivity.desktop.removeCurrentPage()
        }
    }

    fun onSetHomePage() {
        val appSettings = Setup.appSettings()
        appSettings.desktopPageCurrent = homeActivity.desktop.currentItem
    }

    fun onPickWidget() {
        HomeActivity.ignoreResume = true
        val appWidgetId = HomeActivity._appWidgetHost.allocateAppWidgetId()
        val pickIntent = Intent("android.appwidget.action.APPWIDGET_PICK").apply {
            putExtra("appWidgetId", appWidgetId)
        }
        homeActivity.startActivityForResult(pickIntent, REQUEST_PICK_APPWIDGET)
    }

    fun onPickAction() {
        Setup.eventHandler().showPickAction(homeActivity, this)
    }

    fun onLaunchSettings() {
        Setup.eventHandler().showLauncherSettings(homeActivity)
    }

    fun configureWidget(data: Intent?) {
        data ?: return
        val extras = data.extras ?: return
        val appWidgetId = extras.getInt("appWidgetId", -1)
        val appWidgetInfo = AppWidgetManager.getInstance(homeActivity).getAppWidgetInfo(appWidgetId)
        if (appWidgetInfo.configure != null) {
            val intent = Intent("android.appwidget.action.APPWIDGET_CONFIGURE").apply {
                component = appWidgetInfo.configure
                putExtra("appWidgetId", appWidgetId)
            }
            homeActivity.startActivityForResult(intent, REQUEST_CREATE_APPWIDGET)
        } else {
            createWidget(data)
        }
    }

    fun createWidget(data: Intent?) {
        data ?: return
        val extras = data.extras ?: return
        val appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
        val appWidgetInfo = AppWidgetManager.getInstance(homeActivity).getAppWidgetInfo(appWidgetId)
        val item = Item.newWidgetItem(appWidgetInfo.provider, appWidgetId)
        val desktop = homeActivity.desktop
        val pages = desktop.pages
        val currentPage = pages[desktop.currentItem]
        item._spanX = (appWidgetInfo.minWidth - 1) / currentPage.cellWidth + 1
        item._spanY = (appWidgetInfo.minHeight - 1) / currentPage.cellHeight + 1
        val point = desktop.currentPage.findFreeSpace(item._spanX, item._spanY)
        if (point != null) {
            item._x = point.x
            item._y = point.y

            // add item to database
            Setup.dataManager().saveItem(item, desktop.currentItem, Definitions.ItemPosition.Desktop)
            desktop.addItemToPage(item, desktop.currentItem)
        } else {
            Tool.toast(homeActivity, R.string.toast_not_enough_space)
        }
    }

    override fun onAdd(type: Int) {
        val pos = homeActivity.desktop.currentPage.findFreeSpace()
        if (pos != null) {
            homeActivity.desktop.addItemToCell(Item.newActionItem(type), pos.x, pos.y)
        } else {
            Tool.toast(homeActivity, R.string.toast_not_enough_space)
        }
    }
}
