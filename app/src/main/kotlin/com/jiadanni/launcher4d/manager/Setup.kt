package com.jiadanni.launcher4d.manager

import android.content.Context
import com.jiadanni.launcher4d.interfaces.DialogListener
import com.jiadanni.launcher4d.model.Item
import com.jiadanni.launcher4d.util.AppManager
import com.jiadanni.launcher4d.util.AppSettings
import com.jiadanni.launcher4d.util.DatabaseHelper
import com.jiadanni.launcher4d.viewutil.DesktopGestureListener

abstract class Setup {

    abstract fun getAppContext(): Context

    abstract fun getAppSettings(): AppSettings

    abstract fun getDesktopGestureCallback(): DesktopGestureListener.DesktopGestureCallback

    abstract fun getDataManager(): DatabaseHelper

    abstract fun getAppLoader(): AppManager

    abstract fun getEventHandler(): EventHandler

    interface EventHandler {
        fun showLauncherSettings(context: Context)

        fun showPickAction(context: Context, listener: DialogListener.OnActionDialogListener)

        fun showEditDialog(context: Context, item: Item, listener: DialogListener.OnEditDialogListener)

        fun showDeletePackageDialog(context: Context, item: Item)
    }

    companion object {
        private var _setup: Setup? = null

        @JvmStatic
        fun wasInitialised(): Boolean = _setup != null

        @JvmStatic
        fun init(setup: Setup) {
            _setup = setup
        }

        @JvmStatic
        fun get(): Setup {
            return _setup ?: throw RuntimeException("Setup has not been initialised!")
        }

        @JvmStatic
        fun appContext(): Context = get().getAppContext()

        @JvmStatic
        fun appSettings(): AppSettings = get().getAppSettings()

        @JvmStatic
        fun desktopGestureCallback(): DesktopGestureListener.DesktopGestureCallback = get().getDesktopGestureCallback()

        @JvmStatic
        fun dataManager(): DatabaseHelper = get().getDataManager()

        @JvmStatic
        fun appLoader(): AppManager = get().getAppLoader()

        @JvmStatic
        fun eventHandler(): EventHandler = get().getEventHandler()
    }
}
