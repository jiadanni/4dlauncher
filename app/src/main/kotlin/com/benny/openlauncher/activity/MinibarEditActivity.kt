package com.benny.openlauncher.activity

import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.benny.openlauncher.R
import com.benny.openlauncher.util.AppSettings
import com.benny.openlauncher.util.LauncherAction
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import com.mikepenz.fastadapter.items.AbstractItem
import com.mikepenz.fastadapter.drag.ItemTouchCallback
import com.mikepenz.fastadapter.drag.SimpleDragCallback
import java.util.Collections

class MinibarEditActivity : ColorActivity(), ItemTouchCallback {
    private lateinit var toolbar: Toolbar
    private lateinit var enableSwitch: SwitchCompat
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: FastItemAdapter<Item>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_minibar_edit)

        toolbar = findViewById(R.id.toolbar)
        enableSwitch = findViewById(R.id.enableSwitch)
        recyclerView = findViewById(R.id.recyclerView)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        setTitle(R.string.minibar)

        adapter = FastItemAdapter()

        val touchCallback = SimpleDragCallback(this)
        val touchHelper = ItemTouchHelper(touchCallback)
        touchHelper.attachToRecyclerView(recyclerView)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val minibarArrangement = AppSettings.get().minibarArrangement
        for (item in LauncherAction.actionDisplayItems) {
            adapter.add(Item(item, minibarArrangement.contains(item)))
        }

        val minibarEnable = AppSettings.get().minibarEnable
        enableSwitch.isChecked = minibarEnable
        enableSwitch.setText(if (minibarEnable) R.string.on else R.string.off)
        enableSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            buttonView.setText(if (isChecked) R.string.on else R.string.off)
            AppSettings.get().minibarEnable = isChecked
            HomeActivity.launcher?.let { launcher ->
                launcher.closeAppDrawer()
                launcher.drawerLayout.setDrawerLockMode(
                    if (isChecked) DrawerLayout.LOCK_MODE_UNLOCKED
                    else DrawerLayout.LOCK_MODE_LOCKED_CLOSED
                )
            }
        }

        setResult(RESULT_OK)
    }

    override fun onPause() {
        val minibarArrangement = ArrayList<String>()
        for (item in adapter.adapterItems) {
            if (item.enable) {
                minibarArrangement.add(item.item._action.toString())
            }
        }
        AppSettings.get().minibarArrangement = minibarArrangement
        super.onPause()
    }

    override fun onStop() {
        HomeActivity.launcher?.initMinibar()
        super.onStop()
    }

    override fun itemTouchOnMove(oldPosition: Int, newPosition: Int): Boolean {
        Collections.swap(adapter.adapterItems, oldPosition, newPosition)
        adapter.notifyAdapterDataSetChanged()
        return false
    }

    override fun itemTouchDropped(i: Int, i1: Int) {
    }

    class Item(
        val item: LauncherAction.ActionDisplayItem,
        var enable: Boolean
    ) : AbstractItem<Item, Item.ViewHolder>() {

        override fun getType(): Int = 0

        override fun getLayoutRes(): Int = R.layout.item_edit_minibar

        override fun getViewHolder(v: View): ViewHolder = ViewHolder(v)

        override fun bindView(holder: ViewHolder, payloads: List<Any>) {
            holder.label.text = item._label
            holder.description.text = item._description
            holder.icon.setImageResource(item._icon)
            holder.cb.isChecked = enable
            holder.cb.setOnCheckedChangeListener { _, isChecked ->
                enable = isChecked
            }
            super.bindView(holder, payloads)
        }

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val label: TextView = itemView.findViewById(R.id.tv)
            val description: TextView = itemView.findViewById(R.id.tv2)
            val icon: ImageView = itemView.findViewById(R.id.iv)
            val cb: CheckBox = itemView.findViewById(R.id.cb)
        }
    }
}
