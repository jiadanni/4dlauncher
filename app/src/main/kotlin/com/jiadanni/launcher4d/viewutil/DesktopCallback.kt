package com.jiadanni.launcher4d.viewutil

import android.view.View
import com.jiadanni.launcher4d.interfaces.ItemHistory
import com.jiadanni.launcher4d.model.Item

interface DesktopCallback : ItemHistory {
    fun addItemToPoint(item: Item, x: Int, y: Int): Boolean

    fun addItemToPage(item: Item, page: Int): Boolean

    fun addItemToCell(item: Item, x: Int, y: Int): Boolean

    fun removeItem(view: View, animate: Boolean)
}
