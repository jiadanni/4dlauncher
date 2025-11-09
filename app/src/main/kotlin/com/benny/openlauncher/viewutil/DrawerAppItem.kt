package com.benny.openlauncher.viewutil

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.benny.openlauncher.R
import com.benny.openlauncher.manager.Setup
import com.benny.openlauncher.model.App
import com.benny.openlauncher.model.Item
import com.benny.openlauncher.util.DragAction
import com.benny.openlauncher.widget.AppDrawerGrid
import com.benny.openlauncher.widget.AppItemView
import com.mikepenz.fastadapter.items.AbstractItem

class DrawerAppItem(private val app: App) : AbstractItem<DrawerAppItem.ViewHolder>() {
    // TODO merge IconLabelItem and DrawerAppItem into one class
    // they both do the same thing
    // ideally remove all the custom code for AppItemView in favor of the
    // nicer code in IconLabelItem

    override fun getType(): Int = R.id.id_adapter_drawer_app_item

    override fun getLayoutRes(): Int = R.layout.item_app

    override fun getViewHolder(v: View): ViewHolder = ViewHolder(v)

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        val item = Item.newAppItem(app)
        holder.builder
            .setAppItem(item)
            .withOnLongClick(item, DragAction.Action.DRAWER, null)
        super.bindView(holder, payloads)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val appItemView: AppItemView = itemView as AppItemView
        val builder: AppItemView.Builder

        init {
            appItemView.setTargetedWidth(AppDrawerGrid._itemWidth)
            appItemView.setTargetedHeightPadding(AppDrawerGrid._itemHeightPadding)

            builder = AppItemView.Builder(appItemView)
                .setIconSize(Setup.appSettings().iconSize)
                .setLabelVisibility(Setup.appSettings().drawerShowLabel)
                .setTextColor(Setup.appSettings().drawerLabelColor)
        }
    }
}
