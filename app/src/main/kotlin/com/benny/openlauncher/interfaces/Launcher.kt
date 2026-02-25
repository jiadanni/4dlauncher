package com.benny.openlauncher.interfaces

import android.view.View
import androidx.drawerlayout.widget.DrawerLayout
import com.benny.openlauncher.widget.AppDrawerController
import com.benny.openlauncher.widget.Desktop
import com.benny.openlauncher.widget.Dock
import com.benny.openlauncher.widget.GroupPopupView
import com.benny.openlauncher.widget.ItemOptionView
import com.benny.openlauncher.widget.SearchBar

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
