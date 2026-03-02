package com.jiadanni.launcher4d.interfaces

import android.view.View
import androidx.drawerlayout.widget.DrawerLayout
import com.jiadanni.launcher4d.widget.AppDrawerController
import com.jiadanni.launcher4d.widget.Desktop
import com.jiadanni.launcher4d.widget.Dock
import com.jiadanni.launcher4d.widget.GroupPopupView
import com.jiadanni.launcher4d.widget.ItemOptionView
import com.jiadanni.launcher4d.widget.SearchBar

interface Launcher {
    val desktop: Desktop
    val dock: Dock
    val appDrawerController: AppDrawerController
    val drawerLayout: DrawerLayout
    val groupPopup: GroupPopupView
    val itemOptionView: ItemOptionView
    val searchBar: SearchBar

    fun openAppDrawer(view: View? = null, x: Int = 0, y: Int = 0)
    fun closeAppDrawer()
    fun openFeed()
    fun initMinibar()
}
