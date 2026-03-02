package com.jiadanni.launcher4d.activity.homeparts

import com.jiadanni.launcher4d.activity.HomeActivity
import com.jiadanni.launcher4d.manager.Setup
import com.jiadanni.launcher4d.util.Tool
import com.jiadanni.launcher4d.widget.AppDrawerController
import com.jiadanni.launcher4d.widget.PagerIndicator
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
