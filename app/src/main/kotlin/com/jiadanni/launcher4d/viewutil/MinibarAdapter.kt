package com.jiadanni.launcher4d.viewutil

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.jiadanni.launcher4d.R
import com.jiadanni.launcher4d.util.LauncherAction

class MinibarAdapter(
    private val context: Context,
    private val items: List<LauncherAction.ActionDisplayItem>
) : BaseAdapter() {

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): Any? = null

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.item_minibar, parent, false)

        val icon = view.findViewById<ImageView>(R.id.iv)
        val label = view.findViewById<TextView>(R.id.tv)

        val item = items[position]
        icon.setImageResource(item._icon)
        label.text = item._label

        return view
    }
}
