package com.jiadanni.launcher4d.viewutil

import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.graphics.Color
import android.content.Context
import android.view.View
import com.jiadanni.launcher4d.activity.HomeActivity
import com.jiadanni.launcher4d.manager.Setup
import com.jiadanni.launcher4d.model.Item
import com.jiadanni.launcher4d.notifications.NotificationListener
import com.jiadanni.launcher4d.util.Definitions
import com.jiadanni.launcher4d.util.DragAction
import com.jiadanni.launcher4d.util.DragHandler
import com.jiadanni.launcher4d.util.Tool
import com.jiadanni.launcher4d.widget.AppItemView
import com.jiadanni.launcher4d.widget.WidgetContainer
import com.jiadanni.launcher4d.widget.WidgetView
import org.slf4j.LoggerFactory

object ItemViewFactory {
    private val LOG = LoggerFactory.getLogger("ItemViewFactory")

    @JvmStatic
    @JvmOverloads
    fun getItemView(
        context: Context,
        callback: DesktopCallback?,
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
                    view = builder.setAppItem(item).getView()

                    if (Setup.appSettings().notificationStatus) {
                        NotificationListener.setNotificationCallback(
                            app.packageName,
                            view as NotificationListener.NotificationCallback
                        )
                    }
                }
                Item.Type.SHORTCUT -> {
                    view = builder.setShortcutItem(item).getView()
                }
                Item.Type.GROUP -> {
                    view = builder.setGroupItem(context, callback!!, item).getView()
                    view.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                }
                Item.Type.ACTION -> {
                    view = builder.setActionItem(item).getView()
                }
                else -> {
                    // Do nothing for other types
                }
            }
        }

        // associate the View with its corresponding Item data model
        // this association is retrieved in Desktop.kt for item deletion during page removal
        // and in HpDragOption.kt for drag-and-drop operations (e.g., creating groups)
        view?.tag = item

        return view
    }

    @JvmStatic
    fun getWidgetView(
        context: Context,
        callback: DesktopCallback?,
        type: DragAction.Action,
        item: Item
    ): View? {
        if (HomeActivity._appWidgetHost == null) return null

        var appWidgetInfo: AppWidgetProviderInfo? =
            android.appwidget.AppWidgetManager.getInstance(context).getAppWidgetInfo(item.widgetValue)

        // If we can't find the Widget, we don't want to proceed or we'll end up with a phantom on the home screen.
        if (appWidgetInfo == null) {
            if (item._label.contains(Definitions.DELIMITER)) {
                val cnSplit = item._label.split(Definitions.DELIMITER)
                val cn = ComponentName(cnSplit[0], cnSplit[1])

                val appWidgetId = HomeActivity._appWidgetHost.allocateAppWidgetId()
                if (android.appwidget.AppWidgetManager.getInstance(context).bindAppWidgetIdIfAllowed(appWidgetId, cn)) {
                    appWidgetInfo = android.appwidget.AppWidgetManager.getInstance(context).getAppWidgetInfo(appWidgetId)
                    item.widgetValue = appWidgetId
                    Setup.dataManager().updateItem(item)
                } else {
                    LOG.error("Unable to bind app widget id: {}; removing from database", cn)
                    HomeActivity._appWidgetHost.deleteAppWidgetId(appWidgetId)
                    Setup.dataManager().deleteItem(item, false)
                    return null
                }
            } else {
                // Delete the Widget if we don't have enough information to rehydrate it.
                LOG.debug("Unable to identify Widget for rehydration; removing from database")
                Setup.dataManager().deleteItem(item, false)
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

        widgetView.setOnLongClickListener(DragHandler.getLongClick(item, DragAction.Action.DESKTOP, callback, widgetContainer))

        widgetView.post {
            widgetContainer.updateWidgetOption(item)
        }

        return widgetContainer
    }
}
