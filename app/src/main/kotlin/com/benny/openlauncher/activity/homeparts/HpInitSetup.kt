package com.benny.openlauncher.activity.homeparts

import android.content.Context
import com.benny.openlauncher.AppObject
import com.benny.openlauncher.manager.Setup
import com.benny.openlauncher.util.AppManager
import com.benny.openlauncher.util.AppSettings
import com.benny.openlauncher.util.DatabaseHelper
import com.benny.openlauncher.viewutil.DesktopGestureListener.DesktopGestureCallback

class HpInitSetup(context: Context) : Setup() {
    private val appSettings: AppSettings = AppSettings.get()
    private val desktopGestureCallback: HpGestureCallback = HpGestureCallback(appSettings)
    private val dataManager: DatabaseHelper = DatabaseHelper(context.applicationContext)
    private val appLoader: AppManager = AppManager.getInstance(context.applicationContext)
    private val eventHandler: HpEventHandler = HpEventHandler()

    override fun getAppContext(): Context = AppObject.get()

    override fun getAppSettings(): AppSettings = appSettings

    override fun getDesktopGestureCallback(): DesktopGestureCallback = desktopGestureCallback

    override fun getDataManager(): DatabaseHelper = dataManager

    override fun getAppLoader(): AppManager = appLoader

    override fun getEventHandler(): EventHandler = eventHandler
}
