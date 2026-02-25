package com.benny.openlauncher.util

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.AsyncTask
import android.os.Build
import android.os.UserHandle
import android.os.UserManager
import com.benny.openlauncher.activity.HomeActivity
import com.benny.openlauncher.manager.Setup
import com.benny.openlauncher.interfaces.AppDeleteListener
import com.benny.openlauncher.interfaces.AppUpdateListener
import com.benny.openlauncher.model.App
import com.benny.openlauncher.model.Item
import org.slf4j.LoggerFactory
import java.text.Collator

class AppManager(val context: Context) {

    val packageManager: PackageManager = context.packageManager
    private var _apps = mutableListOf<App>()
    private var _nonFilteredApps = mutableListOf<App>()
    val updateListeners = mutableListOf<AppUpdateListener>()
    val deleteListeners = mutableListOf<AppDeleteListener>()
    var recreateAfterGettingApps = false
    private var task: AsyncTask<*, *, *>? = null

    val apps: List<App>
        get() = _apps

    val nonFilteredApps: List<App>
        get() = _nonFilteredApps

    @Deprecated("Use apps property", ReplaceWith("apps"))
    fun getApps(): List<App> = _apps

    @Deprecated("Use nonFilteredApps property", ReplaceWith("nonFilteredApps"))
    fun getNonFilteredApps(): List<App> = _nonFilteredApps

    @Deprecated("Use packageManager property", ReplaceWith("packageManager"))
    fun getPackageManager(): PackageManager = packageManager

    @Deprecated("Use context property", ReplaceWith("context"))
    fun getContext(): Context = context

    fun findApp(intent: Intent?): App? {
        if (intent?.component == null) return null

        val packageName = intent.component!!.packageName
        val className = intent.component!!.className
        return _apps.find { it._className == className && it._packageName == packageName }
    }

    fun init() {
        getAllApps()
    }

    fun getAllApps() {
        when (task?.status) {
            null, AsyncTask.Status.FINISHED -> {
                task = AsyncGetApps().execute()
            }
            AsyncTask.Status.RUNNING -> {
                task?.cancel(false)
                task = AsyncGetApps().execute()
            }
            else -> {
                // Do nothing for PENDING
            }
        }
    }

    fun getAllApps(context: Context, includeHidden: Boolean): List<App> {
        return if (includeHidden) nonFilteredApps else apps
    }

    fun findItemApp(item: Item): App? {
        return findApp(item.intent)
    }

    fun createApp(intent: Intent): App? {
        return try {
            val info = packageManager.resolveActivity(intent, 0)
            val shortcutInfo = Tool.getShortcutInfo(context, intent.component!!.packageName)
            App(packageManager, info, shortcutInfo)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun onAppUpdated(context: Context, intent: Intent) {
        getAllApps()
    }

    fun addUpdateListener(updateListener: AppUpdateListener) {
        updateListeners.add(updateListener)
    }

    fun addDeleteListener(deleteListener: AppDeleteListener) {
        deleteListeners.add(deleteListener)
    }

    fun notifyUpdateListeners(apps: List<App>) {
        val iter = updateListeners.iterator()
        while (iter.hasNext()) {
            if (iter.next().onAppUpdated(apps)) {
                iter.remove()
            }
        }
    }

    fun notifyRemoveListeners(apps: List<App>) {
        val iter = deleteListeners.iterator()
        while (iter.hasNext()) {
            if (iter.next().onAppDeleted(apps)) {
                iter.remove()
            }
        }
    }

    @Suppress("DEPRECATION")
    private inner class AsyncGetApps : AsyncTask<Any, Any, Any>() {
        private var appsTemp = mutableListOf<App>()
        private var nonFilteredAppsTemp = mutableListOf<App>()
        private var removedApps = mutableListOf<App>()

        override fun onPreExecute() {
            appsTemp = mutableListOf()
            nonFilteredAppsTemp = mutableListOf()
            removedApps = mutableListOf()
            super.onPreExecute()
        }

        override fun onCancelled() {
            appsTemp.clear()
            nonFilteredAppsTemp.clear()
            removedApps = mutableListOf()
            super.onCancelled()
        }

        override fun doInBackground(vararg params: Any?): Any? {
            // work profile support
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                    val profiles = launcherApps.profiles
                    for (userHandle in profiles) {
                        val apps = launcherApps.getActivityList(null, userHandle)
                        for (info in apps) {
                            val shortcutInfo = Tool.getShortcutInfo(context, info.componentName.packageName)
                            val app = App(packageManager, info, shortcutInfo).apply {
                                _userHandle = userHandle
                            }
                            LOG.debug("adding work profile to non filtered list: {}, {}, {}", app._label, app._packageName, app._className)
                            nonFilteredAppsTemp.add(app)
                        }
                    }
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 -> {
                    val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
                    // LauncherApps.getProfiles() is not available for API 25, so just get all associated user profile handlers
                    val profiles = userManager.userProfiles
                    val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                    for (userHandle in profiles) {
                        val apps = launcherApps.getActivityList(null, userHandle)
                        for (info in apps) {
                            val shortcutInfo = Tool.getShortcutInfo(context, info.componentName.packageName)
                            val app = App(packageManager, info, shortcutInfo).apply {
                                _userHandle = userHandle
                            }
                            LOG.debug("adding work profile to non filtered list: {}, {}, {}", app._label, app._packageName, app._className)
                            nonFilteredAppsTemp.add(app)
                        }
                    }
                }
                else -> {
                    val intent = Intent(Intent.ACTION_MAIN, null).apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                    }
                    val activitiesInfo = packageManager.queryIntentActivities(intent, 0)
                    for (info in activitiesInfo) {
                        val app = App(packageManager, info, null)
                        LOG.debug("adding app to non filtered list: {}, {}, {}", app._label, app._packageName, app._className)
                        nonFilteredAppsTemp.add(app)
                    }
                }
            }

            // sort the apps by label here
            nonFilteredAppsTemp.sortWith { one, two ->
                Collator.getInstance().compare(one._label, two._label)
            }

            val hiddenList = AppSettings.get().hiddenAppsList
            if (hiddenList != null) {
                for (i in nonFilteredAppsTemp.indices) {
                    val shouldGetAway = hiddenList.any { hidItemRaw ->
                        nonFilteredAppsTemp[i].componentName == hidItemRaw
                    }
                    if (!shouldGetAway) {
                        appsTemp.add(nonFilteredAppsTemp[i])
                    }
                }
            } else {
                appsTemp.addAll(nonFilteredAppsTemp)
            }

            removedApps = getRemovedApps(_apps, appsTemp).toMutableList()

            for (app in removedApps) {
                Setup.dataManager().deleteItems(app)
            }

            val appSettings = AppSettings.get()
            if (appSettings.iconPack.isNotEmpty() && Tool.isPackageInstalled(appSettings.iconPack, packageManager)) {
                IconPackHelper.applyIconPack(this@AppManager, Tool.dp2px(appSettings.iconSize.toFloat()), appSettings.iconPack, appsTemp)
            }
            return null
        }

        override fun onPostExecute(result: Any?) {
            _apps = appsTemp
            _nonFilteredApps = nonFilteredAppsTemp

            if (removedApps.size > 0) {
                notifyRemoveListeners(removedApps)
            }

            notifyUpdateListeners(appsTemp)

            if (recreateAfterGettingApps) {
                recreateAfterGettingApps = false
                if (context is HomeActivity) {
                    context.recreate()
                }
            }

            super.onPostExecute(result)
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger("AppManager")

        private var appManager: AppManager? = null

        @JvmStatic
        fun getInstance(context: Context): AppManager {
            return appManager ?: AppManager(context.applicationContext).also { appManager = it }
        }

        @JvmStatic
        fun getRemovedApps(oldApps: List<App>, newApps: List<App>): List<App> {
            val removed = mutableListOf<App>()
            // if this is the first call then return an empty list
            if (oldApps.isEmpty()) {
                return removed
            }
            for (i in oldApps.indices) {
                if (!newApps.contains(oldApps[i])) {
                    removed.add(oldApps[i])
                    break
                }
            }
            return removed
        }
    }
}
