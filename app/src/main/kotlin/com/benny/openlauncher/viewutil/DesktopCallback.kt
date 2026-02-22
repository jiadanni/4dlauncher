package com.benny.openlauncher.viewutil

import android.view.View
import com.benny.openlauncher.interfaces.ItemHistory
import com.benny.openlauncher.model.Item

interface DesktopCallback : ItemHistory {
    fun addItemToPoint(item: Item, x: Int, y: Int): Boolean

    fun addItemToPage(item: Item, page: Int): Boolean

    fun addItemToCell(item: Item, x: Int, y: Int): Boolean

    fun removeItem(view: View, animate: Boolean)
}
