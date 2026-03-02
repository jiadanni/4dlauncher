package com.jiadanni.launcher4d.activity.homeparts

import android.content.Context
import com.jiadanni.launcher4d.AppObject
import com.jiadanni.launcher4d.manager.Setup
import com.jiadanni.launcher4d.util.AppManager
import com.jiadanni.launcher4d.util.AppSettings
import com.jiadanni.launcher4d.util.DatabaseHelper
import com.jiadanni.launcher4d.viewutil.DesktopGestureListener.DesktopGestureCallback

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
