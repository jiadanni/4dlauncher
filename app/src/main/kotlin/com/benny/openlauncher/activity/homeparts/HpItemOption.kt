package com.benny.openlauncher.activity.homeparts

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Point
import android.net.Uri
import android.os.Build
import com.benny.openlauncher.R
import com.benny.openlauncher.activity.HomeActivity
import com.benny.openlauncher.interfaces.DialogListener
import com.benny.openlauncher.manager.Setup
import com.benny.openlauncher.model.Item
import com.benny.openlauncher.util.Definitions.ItemPosition
import com.benny.openlauncher.util.Tool
import com.benny.openlauncher.widget.WidgetContainer

class HpItemOption(
    private val homeActivity: HomeActivity
) : DialogListener.OnEditDialogListener {

    private var item: Item? = null

    fun onEditItem(item: Item) {
        this.item = item
        Setup.eventHandler().showEditDialog(homeActivity, item, this)
    }

    fun onUninstallItem(item: Item) {
        HomeActivity.ignoreResume = true
        Setup.eventHandler().showDeletePackageDialog(homeActivity, item)
    }

    fun onRemoveItem(item: Item) {
        when (item._location) {
            ItemPosition.Group -> {
                Tool.toast(homeActivity, R.string.toast_remove_from_group_first)
                return
            }
            ItemPosition.Desktop -> {
                val desktop = homeActivity.desktop
                val view = desktop.currentPage.coordinateToChildView(Point(item._x, item._y))
                desktop.removeItem(view, true)
            }
            else -> {
                val dock = homeActivity.dock
                val view = dock.coordinateToChildView(Point(item._x, item._y))
                dock.removeItem(view, true)
            }
        }
        HomeActivity._db.deleteItem(item, true)
    }

    fun onInfoItem(item: Item) {
        if (item._type == Item.Type.APP) {
            try {
                val intent = Intent(
                    "android.settings.APPLICATION_DETAILS_SETTINGS",
                    Uri.parse("package:${item._intent?.component?.packageName}")
                )
                homeActivity.startActivity(intent)
            } catch (e: Exception) {
                Tool.toast(homeActivity, R.string.toast_app_uninstalled)
            }
        }
    }

    fun onResizeItem(item: Item) {
        val coordinateToChildView = if (item._location == ItemPosition.Desktop) {
            val desktop = homeActivity.desktop
            desktop.currentPage.coordinateToChildView(Point(item._x, item._y))
        } else {
            val dock = homeActivity.dock
            dock.coordinateToChildView(Point(item._x, item._y))
        }

        (coordinateToChildView as? WidgetContainer)?.showResize()
    }

    fun onStartShortcutItem(item: Item, position: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            val launcherApps = homeActivity.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val shortcutInfo = item.shortcutInfo[position]
            launcherApps.startShortcut(shortcutInfo, null, null)
        }
    }

    override fun onRename(name: String) {
        val currentItem = item ?: return
        currentItem.label = name
        Setup.dataManager().saveItem(currentItem)
        val point = Point(currentItem._x, currentItem._y)

        when (currentItem._location) {
            ItemPosition.Group -> return
            ItemPosition.Desktop -> {
                val desktop = homeActivity.desktop
                desktop.removeItem(desktop.currentPage.coordinateToChildView(point), false)
                desktop.addItemToCell(currentItem, currentItem._x, currentItem._y)
            }
            else -> {
                val dock = homeActivity.dock
                homeActivity.dock.removeItem(dock.coordinateToChildView(point), false)
                dock.addItemToCell(currentItem, currentItem._x, currentItem._y)
            }
        }
    }
}
