package com.benny.openlauncher.viewutil

import android.graphics.drawable.Drawable
import android.view.View
import com.benny.openlauncher.R

class PopupDynamicIconLabelItem(
    private val label: CharSequence,
    private val icon: Drawable
) : AbstractPopupIconLabelItem<PopupDynamicIconLabelItem>() {

    override fun getType(): Int = R.id.id_adapter_popup_icon_label_item

    override fun getLayoutRes(): Int = R.layout.item_popup_icon_label

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)

        holder.labelView.text = label
        holder.iconView.setImageDrawable(icon)
    }

    override fun unbindView(holder: ViewHolder) {
        super.unbindView(holder)

        holder.labelView.text = null
        holder.iconView.setImageDrawable(null)
    }

    override fun getViewHolder(view: View): ViewHolder = ViewHolder(view)
}
