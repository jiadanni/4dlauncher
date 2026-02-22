package com.benny.openlauncher.viewutil

import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.graphics.Color
import android.content.Context
import android.view.View
import com.benny.openlauncher.activity.HomeActivity
import com.benny.openlauncher.manager.Setup
import com.benny.openlauncher.model.Item
import com.benny.openlauncher.notifications.NotificationListener
import com.benny.openlauncher.util.Definitions
import com.benny.openlauncher.util.DragAction
import com.benny.openlauncher.util.DragHandler
import com.benny.openlauncher.util.Tool
import com.benny.openlauncher.widget.AppItemView
import com.benny.openlauncher.widget.WidgetContainer
import com.benny.openlauncher.widget.WidgetView
import org.slf4j.LoggerFactory

object ItemViewFactory {
    private val LOG = LoggerFactory.getLogger("ItemViewFactory")

    @JvmStatic
    @JvmOverloads
    fun getItemView(
        context: Context,
        callback: DesktopCallback,
        type: DragAction.Action,
        item: Item,
        showLabel: Boolean? = null
    ): View? {
        var view: View? = null

        if (item.type == Item.Type.WIDGET) {
            view = getWidgetView(context, callback, type, item)
        } else {
            val builder = AppItemView.Builder(context)
                .setIconSize(Setup.appSettings().iconSize)
                .vibrateWhenLongPress(Setup.appSettings().gestureFeedback)
                .withOnLongClick(item, type, callback)

            when (type) {
                DragAction.Action.DRAWER -> {
                    builder.setLabelVisibility(Setup.appSettings().drawerShowLabel)
                    builder.setTextColor(Setup.appSettings().drawerLabelColor)
                }
                DragAction.Action.DESKTOP,
                DragAction.Action.SEARCH -> {
                    builder.setLabelVisibility(Setup.appSettings().desktopShowLabel)
                    builder.setTextColor(Color.WHITE)
                }
            }

            showLabel?.let { builder.setLabelVisibility(it) }

            when (item.type) {
                Item.Type.APP -> {
                    val app = Setup.appLoader().findItemApp(item) ?: return null
                    view = builder.setAppItem(item).view

                    if (Setup.appSettings().notificationStatus) {
                        NotificationListener.setNotificationCallback(
                            app.packageName,
                            view as NotificationListener.NotificationCallback
                        )
                    }
                }
                Item.Type.SHORTCUT -> {
                    view = builder.setShortcutItem(item).view
                }
                Item.Type.GROUP -> {
                    view = builder.setGroupItem(context, callback, item).view
                    view.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                }
                Item.Type.ACTION -> {
                    view = builder.setActionItem(item).view
                }
                else -> {
                    // Do nothing for other types
                }
            }
        }

        // TODO find out why tag is set here
        view?.tag = item

        return view
    }

    @JvmStatic
    fun getWidgetView(
        context: Context,
        callback: DesktopCallback,
        type: DragAction.Action,
        item: Item
    ): View? {
        if (HomeActivity._appWidgetHost == null) return null

        var appWidgetInfo: AppWidgetProviderInfo? =
            HomeActivity._appWidgetManager.getAppWidgetInfo(item.widgetValue)

        // If we can't find the Widget, we don't want to proceed or we'll end up with a phantom on the home screen.
        if (appWidgetInfo == null) {
            if (item._label.contains(Definitions.DELIMITER)) {
                val cnSplit = item._label.split(Definitions.DELIMITER)
                val cn = ComponentName(cnSplit[0], cnSplit[1])

                val appWidgetId = HomeActivity._appWidgetHost.allocateAppWidgetId()
                if (HomeActivity._appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, cn)) {
                    appWidgetInfo = HomeActivity._appWidgetManager.getAppWidgetInfo(appWidgetId)
                    item.widgetValue = appWidgetId
                    HomeActivity._db.updateItem(item)
                } else {
                    LOG.error("Unable to bind app widget id: {}; removing from database", cn)
                    HomeActivity._appWidgetHost.deleteAppWidgetId(appWidgetId)
                    HomeActivity._db.deleteItem(item, false)
                    return null
                }
            } else {
                // Delete the Widget if we don't have enough information to rehydrate it.
                LOG.debug("Unable to identify Widget for rehydration; removing from database")
                HomeActivity._db.deleteItem(item, false)
                return null
            }
        }

        val widgetView = HomeActivity._appWidgetHost.createView(
            context,
            item.widgetValue,
            appWidgetInfo
        ) as WidgetView
        widgetView.setAppWidget(item.widgetValue, appWidgetInfo)

        val widgetContainer = WidgetContainer(context, widgetView, item)

        // TODO move this to standard DragHandler.getLongClick() method
        // needs to be set on widgetView but use widgetContainer inside
        widgetView.setOnLongClickListener { view ->
            if (Setup.appSettings().desktopLock) {
                return@setOnLongClickListener false
            }
            if (Setup.appSettings().gestureFeedback) {
                Tool.vibrate(view)
            }
            DragHandler.startDrag(widgetContainer, item, DragAction.Action.DESKTOP, callback)
            true
        }

        widgetView.post {
            widgetContainer.updateWidgetOption(item)
        }

        return widgetContainer
    }
}
