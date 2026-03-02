package com.jiadanni.launcher4d.interfaces

import android.view.View
import com.jiadanni.launcher4d.model.Item

interface ItemHistory {
    fun setLastItem(item: Item, view: View)

    fun revertLastItem()

    fun consumeLastItem()
}
