package com.jiadanni.launcher4d.viewutil

import android.view.View
import com.jiadanni.launcher4d.R

class PopupIconLabelItem(
    private val labelRes: Int,
    private val iconRes: Int
) : AbstractPopupIconLabelItem() {

    override val type: Int get() = R.id.id_adapter_popup_icon_label_item

    override val layoutRes: Int get() = R.layout.item_popup_icon_label

    override fun bindView(holder: ViewHolder, payloads: List<Any>) {
        super.bindView(holder, payloads)

        holder.labelView.setText(labelRes)
        holder.iconView.setImageResource(iconRes)
    }

    override fun unbindView(holder: ViewHolder) {
        super.unbindView(holder)

        holder.labelView.text = null
        holder.iconView.setImageDrawable(null)
    }

    override fun getViewHolder(view: View): ViewHolder = ViewHolder(view)
}
