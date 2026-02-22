package com.benny.openlauncher.activity.homeparts

import com.benny.openlauncher.activity.HomeActivity
import com.benny.openlauncher.manager.Setup
import com.benny.openlauncher.util.Tool
import com.benny.openlauncher.widget.AppDrawerController
import com.benny.openlauncher.widget.PagerIndicator
import net.gsantner.opoc.util.Callback

class HpAppDrawer(
    private val homeActivity: HomeActivity,
    private val appDrawerIndicator: PagerIndicator
) : Callback.a2<Boolean, Boolean> {

    fun initAppDrawer(appDrawerController: AppDrawerController) {
        appDrawerController.setCallBack(this)
    }

    override fun callback(openingOrClosing: Boolean, startOrEnd: Boolean) {
        if (openingOrClosing) {
            if (startOrEnd) {
                homeActivity.appDrawerController.postDelayed({
                    Tool.visibleViews(200, appDrawerIndicator)
                    Tool.invisibleViews(200, homeActivity.desktop)
                    homeActivity.updateDesktopIndicator(false)
                    homeActivity.updateDock(false)
                    homeActivity.updateSearchBar(false)
                }, 100)
            }
        } else {
            if (startOrEnd) {
                Tool.invisibleViews(200, appDrawerIndicator)
                Tool.visibleViews(200, homeActivity.desktop)
                homeActivity.updateDesktopIndicator(true)
                homeActivity.updateDock(true)
                homeActivity.updateSearchBar(true)
            } else {
                if (!Setup.appSettings().drawerRememberPosition) {
                    homeActivity.appDrawerController.reset()
                }
            }
        }
    }
}
