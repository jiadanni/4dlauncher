package com.benny.openlauncher.interfaces

import android.view.View
import com.benny.openlauncher.model.Item

interface ItemHistory {
    fun setLastItem(item: Item, view: View)

    fun revertLastItem()

    fun consumeLastItem()
}
