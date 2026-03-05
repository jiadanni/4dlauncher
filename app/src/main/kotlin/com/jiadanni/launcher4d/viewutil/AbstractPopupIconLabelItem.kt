package com.jiadanni.launcher4d.viewutil

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.jiadanni.launcher4d.R
import com.mikepenz.fastadapter.IClickable
import com.mikepenz.fastadapter.IItem
import com.mikepenz.fastadapter.items.AbstractItem

abstract class AbstractPopupIconLabelItem : AbstractItem<AbstractPopupIconLabelItem.ViewHolder>()
         {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconView: ImageView = itemView.findViewById(R.id.item_popup_icon)
        val labelView: TextView = itemView.findViewById(R.id.item_popup_label)
    }
}
