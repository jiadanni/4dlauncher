package com.jiadanni.launcher4d.viewutil

import android.graphics.drawable.Drawable
import android.view.View
import com.jiadanni.launcher4d.R

class PopupDynamicIconLabelItem(
    private val label: CharSequence,
    private val icon: Drawable
) : AbstractPopupIconLabelItem() {

    override val type: Int get() = R.id.id_adapter_popup_icon_label_item

    override val layoutRes: Int get() = R.layout.item_popup_icon_label

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
