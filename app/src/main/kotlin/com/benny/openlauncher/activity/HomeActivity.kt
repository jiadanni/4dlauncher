package com.benny.openlauncher.activity

import android.app.Activity
import android.app.ActivityOptions
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.FrameLayout
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.NotificationManagerCompat
import androidx.viewpager.widget.ViewPager
import com.benny.openlauncher.BuildConfig
import com.benny.openlauncher.R
import com.benny.openlauncher.activity.homeparts.HpAppDrawer
import com.benny.openlauncher.activity.homeparts.HpDesktopOption
import com.benny.openlauncher.activity.homeparts.HpDragOption
import com.benny.openlauncher.activity.homeparts.HpInitSetup
import com.benny.openlauncher.activity.homeparts.HpSearchBar
import com.benny.openlauncher.feed.FeedPermissionManager
import com.benny.openlauncher.interfaces.AppDeleteListener
import com.benny.openlauncher.interfaces.AppUpdateListener
import com.benny.openlauncher.interfaces.Launcher
import com.benny.openlauncher.manager.Setup
import com.benny.openlauncher.model.App
import com.benny.openlauncher.model.Item
import com.benny.openlauncher.notifications.NotificationListener
import com.benny.openlauncher.receivers.AppUpdateReceiver
import com.benny.openlauncher.util.AppManager
import com.benny.openlauncher.util.AppSettings
import com.benny.openlauncher.util.DatabaseHelper
import com.benny.openlauncher.util.Definitions.ItemPosition
import com.benny.openlauncher.util.LauncherAction
import com.benny.openlauncher.util.LauncherAction.Action
import com.benny.openlauncher.util.Tool
import com.benny.openlauncher.viewutil.DialogHelper
import com.benny.openlauncher.viewutil.MinibarAdapter
import com.benny.openlauncher.viewutil.WidgetHost
import com.benny.openlauncher.widget.AppDrawerController
import com.benny.openlauncher.widget.AppItemView
import com.benny.openlauncher.widget.Desktop
import com.benny.openlauncher.widget.Desktop.OnDesktopEditListener
import com.benny.openlauncher.widget.DesktopOptionView
import com.benny.openlauncher.widget.Dock
import com.benny.openlauncher.widget.GroupPopupView
import com.benny.openlauncher.widget.ItemOptionView
import com.benny.openlauncher.widget.MinibarView
import com.benny.openlauncher.widget.PagerIndicator
import com.benny.openlauncher.widget.SearchBar
import com.jakewharton.threetenabp.AndroidThreeTen
import net.gsantner.opoc.util.ContextUtils

class HomeActivity : Activity(), OnDesktopEditListener, Launcher {

    private var cx: Int = 0
    private var cy: Int = 0

    private lateinit var appUpdateReceiver: AppUpdateReceiver
    private lateinit var timeChangedReceiver: BroadcastReceiver
    private lateinit var feedPermissionManager: FeedPermissionManager

    override val drawerLayout: DrawerLayout
        get() = findViewById(R.id.drawer_layout)

    override val desktop: Desktop
        get() = findViewById(R.id.desktop)

    override val dock: Dock
        get() = findViewById(R.id.dock)

    override val appDrawerController: AppDrawerController
        get() = findViewById(R.id.appDrawerController)

    override val groupPopup: GroupPopupView
        get() = findViewById(R.id.groupPopup)

    override val searchBar: SearchBar
        get() = findViewById(R.id.searchBar)

    val background: View
        get() = findViewById(R.id.background_frame)

    val desktopIndicator: PagerIndicator
        get() = findViewById(R.id.desktopIndicator)

    val desktopOptionView: DesktopOptionView
        get() = findViewById(R.id.desktop_option)

    override val itemOptionView: ItemOptionView
        get() = findViewById(R.id.item_option)

    val minibarFrame: FrameLayout
        get() = findViewById(R.id.minibar_frame)

    val statusView: View
        get() = findViewById(R.id.status_frame)

    val navigationView: View
        get() = findViewById(R.id.navigation_frame)

    val feedView: com.benny.openlauncher.widget.FeedView
        get() = findViewById(R.id.feed_view)

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidThreeTen.init(this)

        val appSettings = AppSettings.get()

        val contextUtils = ContextUtils(applicationContext)
        contextUtils.setAppLanguage(appSettings.language)
        super.onCreate(savedInstanceState)

        if (!Setup.wasInitialised()) {
            Setup.init(HpInitSetup(this))
        }

        setContentView(layoutInflater.inflate(R.layout.activity_home, null))

        // transparent status and navigation
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.decorView.systemUiVisibility = 1536
        }

        init()
    }

    private fun init() {
        _appWidgetManager = AppWidgetManager.getInstance(this)
        _appWidgetHost = WidgetHost(applicationContext, R.id.app_widget_host)
        _appWidgetHost.startListening()

        // Initialize feed permission manager
        feedPermissionManager = FeedPermissionManager(this)

        // item drag and drop
        val hpDragOption = HpDragOption()
        val leftDragHandle = findViewById<View>(R.id.leftDragHandle)
        val rightDragHandle = findViewById<View>(R.id.rightDragHandle)
        hpDragOption.initDragNDrop(this, leftDragHandle, rightDragHandle, itemOptionView)

        registerBroadcastReceiver()
        initAppManager()
        initSettings()
        initViews()
    }

    protected fun initAppManager() {
        if (Setup.appSettings().appFirstLaunch) {
            Setup.appSettings().appFirstLaunch = false
            Setup.appSettings().isAppShowIntro = false
            val appDrawerBtnItem = Item.newActionItem(8)
            appDrawerBtnItem._x = 2
            Setup.dataManager().saveItem(appDrawerBtnItem, 0, ItemPosition.Dock)
        }

        Setup.appLoader().addUpdateListener(object : AppUpdateListener {
            override fun onAppUpdated(it: List<App>): Boolean {
                desktop.initDesktop()
                dock.initDock()
                return false
            }
        })

        Setup.appLoader().addDeleteListener(object : AppDeleteListener {
            override fun onAppDeleted(apps: List<App>): Boolean {
                desktop.initDesktop()
                dock.initDock()
                return false
            }
        })

        AppManager.getInstance(this).init()
    }

    protected fun initViews() {
        HpSearchBar(this, searchBar).initSearchBar()
        appDrawerController.init()
        dock.setHome(this)

        desktop.setDesktopEditListener(this)
        desktop.setPageIndicator(desktopIndicator)
        desktopIndicator.setMode(Setup.appSettings().desktopIndicatorMode)

        val appSettings = Setup.appSettings()

        _desktopOption = HpDesktopOption(this)

        desktopOptionView.setDesktopOptionViewListener(_desktopOption)
        desktopOptionView.postDelayed({
            desktopOptionView.updateLockIcon(appSettings.desktopLock)
        }, 100)

        desktop.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageSelected(position: Int) {
                desktopOptionView.updateHomeIcon(appSettings.desktopPageCurrent == position)
            }

            override fun onPageScrollStateChanged(state: Int) {
            }
        })

        HpAppDrawer(this, findViewById(R.id.appDrawerIndicator)).initAppDrawer(appDrawerController)
        initMinibar()
    }

    override fun initMinibar() {
        val items = AppSettings.get().minibarArrangement
        val minibar = findViewById<MinibarView>(R.id.minibar)
        minibar.adapter = MinibarAdapter(this, items)
        minibar.onItemClickListener = AdapterView.OnItemClickListener { _, _, i, _ ->
            LauncherAction.RunAction(items[i], this@HomeActivity)
        }
    }

    fun initSettings() {
        updateHomeLayout()

        val appSettings = Setup.appSettings()
        if (appSettings.desktopFullscreen) {
            window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }

        // set background colors
        desktop.setBackgroundColor(appSettings.desktopBackgroundColor)
        dock.setBackgroundColor(appSettings.dockColor)

        // set frame colors
        minibarFrame.setBackgroundColor(appSettings.minibarBackgroundColor)
        statusView.setBackgroundColor(appSettings.desktopInsetColor)
        navigationView.setBackgroundColor(appSettings.desktopInsetColor)

        // lock the minibar
        drawerLayout.setDrawerLockMode(
            if (appSettings.minibarEnable) DrawerLayout.LOCK_MODE_UNLOCKED
            else DrawerLayout.LOCK_MODE_LOCKED_CLOSED
        )
    }

    private fun registerBroadcastReceiver() {
        appUpdateReceiver = AppUpdateReceiver()
        timeChangedReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_TIME_TICK,
                    Intent.ACTION_TIMEZONE_CHANGED,
                    Intent.ACTION_TIME_CHANGED -> updateSearchClock()
                }
            }
        }

        // register all receivers
        registerReceiver(appUpdateReceiver, _appUpdateIntentFilter)
        registerReceiver(timeChangedReceiver, _timeChangedIntentFilter)
    }

    fun onStartApp(context: Context, app: App, view: View?) {
        if (BuildConfig.APPLICATION_ID == app._packageName) {
            LauncherAction.RunAction(Action.LauncherSettings, context)
            return
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && app._userHandle != null) {
                val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
                val activities = launcherApps.getActivityList(app.packageName, app._userHandle)
                for (activity in activities) {
                    if (app.componentName == activity.componentName.toString()) {
                        launcherApps.startMainActivity(
                            activity.componentName,
                            app._userHandle,
                            null,
                            getActivityAnimationOpts(view)
                        )
                    }
                }
            } else {
                val intent = Tool.getIntentFromApp(app)
                context.startActivity(intent, getActivityAnimationOpts(view))
            }

            // close app drawer and other items in advance
            // annoying to wait for app drawer to close
            handleLauncherResume()
        } catch (e: Exception) {
            e.printStackTrace()
            Tool.toast(context, R.string.toast_app_uninstalled)
        }
    }

    private fun getActivityAnimationOpts(view: View?): Bundle? {
        view ?: return null

        var options: ActivityOptions? = null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var left = 0
            var top = 0
            var width = view.measuredWidth
            var height = view.measuredHeight

            if (view is AppItemView) {
                width = view.iconSize.toInt()
                left = view.drawIconLeft.toInt()
                top = view.drawIconTop.toInt()
            }
            options = ActivityOptions.makeClipRevealAnimation(view, left, top, width, height)
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            options = ActivityOptions.makeScaleUpAnimation(view, 0, 0, view.measuredWidth, view.measuredHeight)
        }

        return options?.toBundle()
    }

    override fun onStartDesktopEdit() {
        Tool.visibleViews(100, desktopOptionView)
        updateDesktopIndicator(false)
        updateDock(false)
        updateSearchBar(false)
    }

    override fun onFinishDesktopEdit() {
        Tool.invisibleViews(100, desktopOptionView)
        updateDesktopIndicator(true)
        updateDock(true)
        updateSearchBar(true)
    }

    fun dimBackground() {
        Tool.visibleViews(200, background)
    }

    fun unDimBackground() {
        Tool.invisibleViews(200, background)
    }

    fun clearRoomForPopUp() {
        Tool.invisibleViews(200, desktop)
        updateDesktopIndicator(false)
        updateDock(false)
    }

    fun unClearRoomForPopUp() {
        Tool.visibleViews(200, desktop)
        updateDesktopIndicator(true)
        updateDock(true)
    }

    fun updateDock(show: Boolean) {
        val appSettings = Setup.appSettings()
        if (appSettings.dockEnable && show) {
            Tool.visibleViews(100, dock)
        } else {
            if (appSettings.dockEnable) {
                Tool.invisibleViews(100, dock)
            } else {
                Tool.goneViews(100, dock)
            }
        }
    }

    fun updateSearchBar(show: Boolean) {
        val appSettings = Setup.appSettings()
        if (appSettings.searchBarEnable && show) {
            Tool.visibleViews(100, searchBar)
        } else {
            if (appSettings.searchBarEnable) {
                Tool.invisibleViews(100, searchBar)
            } else {
                Tool.goneViews(100, searchBar)
            }
        }
    }

    fun updateDesktopIndicator(show: Boolean) {
        val appSettings = Setup.appSettings()
        if (appSettings.desktopShowIndicator && show) {
            Tool.visibleViews(100, desktopIndicator)
        } else {
            Tool.goneViews(100, desktopIndicator)
        }
    }

    fun updateSearchClock() {
        val textView = searchBar._searchClock

        if (textView.text != null) {
            try {
                searchBar.updateClock()
            } catch (e: Exception) {
                searchBar._searchClock.setText(R.string.bad_format)
            }
        }
    }

    fun updateHomeLayout() {
        updateSearchBar(true)
        updateDock(true)
        updateDesktopIndicator(true)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_PICK_APPWIDGET -> _desktopOption?.configureWidget(data)
                REQUEST_CREATE_APPWIDGET -> _desktopOption?.createWidget(data)
            }
        } else if (resultCode == RESULT_CANCELED && data != null) {
            val appWidgetId = data.getIntExtra("appWidgetId", -1)
            if (appWidgetId != -1) {
                _appWidgetHost.deleteAppWidgetId(appWidgetId)
            }
        }
    }

    override fun onBackPressed() {
        handleLauncherResume()
    }

    override fun onStart() {
        _appWidgetHost.startListening()

        super.onStart()
    }

    private fun checkNotificationPermissions() {
        val appList = NotificationManagerCompat.getEnabledListenerPackages(this)
        for (app in appList) {
            if (app == packageName) {
                // Already allowed, so request a full update when returning to the home screen from another app.
                val i = Intent(NotificationListener.UPDATE_NOTIFICATIONS_ACTION).apply {
                    setPackage(packageName)
                    putExtra(NotificationListener.UPDATE_NOTIFICATIONS_COMMAND, NotificationListener.UPDATE_NOTIFICATIONS_UPDATE)
                }
                sendBroadcast(i)
                return
            }
        }

        // Request the required permission otherwise.
        DialogHelper.alertDialog(
            this,
            getString(R.string.notification_title),
            getString(R.string.notification_summary),
            getString(R.string.enable)
        ) { _, _ ->
            Tool.toast(this@HomeActivity, getString(R.string.toast_notification_permission_required))
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Handle assist intent to open feed
        if (intent?.action == Intent.ACTION_ASSIST) {
            openFeed()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        // Handle feed permission results
        val result = feedPermissionManager.handlePermissionResult(requestCode, permissions, grantResults)

        when (result) {
            is FeedPermissionManager.PermissionResult.ALL_GRANTED -> {
                // All permissions granted, open the feed
                Tool.toast(this, "Feed permissions granted")
                drawerLayout.openDrawer(androidx.core.view.GravityCompat.END)
                feedView.refreshFeed()
            }
            is FeedPermissionManager.PermissionResult.PARTIALLY_DENIED -> {
                // Some permissions denied, show explanation
                showFeedPermissionDenied(result.deniedPermissions)
            }
            is FeedPermissionManager.PermissionResult.NOT_HANDLED -> {
                // Not our request code, ignore
            }
        }
    }

    override fun onResume() {
        super.onResume()
        _appWidgetHost.startListening()

        // handle restart if something needs to be reset
        val appSettings = Setup.appSettings()
        if (appSettings.appRestartRequired) {
            appSettings.appRestartRequired = false
            recreate()
            return
        }

        if (appSettings.notificationStatus) {
            // Ask user to allow the Notification permission if not already provided.
            checkNotificationPermissions()
        }

        initMinibar()
        initSettings()

        // handle launcher rotation
        requestedOrientation = when (appSettings.desktopOrientationMode) {
            2 -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            1 -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
            else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        handleLauncherResume()
    }

    override fun onDestroy() {
        _appWidgetHost.stopListening()

        unregisterReceiver(appUpdateReceiver)
        unregisterReceiver(timeChangedReceiver)
        super.onDestroy()
    }

    private fun handleLauncherResume() {
        if (ignoreResume) {
            // only triggers when a new activity is launched that should leave launcher state alone
            // uninstall package activity and pick widget activity
            ignoreResume = false
        } else {
            searchBar.collapse()
            groupPopup.collapse()
            // close app option menu
            itemOptionView.collapse()
            // close minibar
            drawerLayout.closeDrawers()
            if (desktop.inEditMode) {
                // exit desktop edit mode
                desktop.currentPage.performClick()
            } else if (appDrawerController.drawer.visibility == View.VISIBLE) {
                closeAppDrawer()
            }
            if (desktop.currentItem != 0) {
                val appSettings = Setup.appSettings()
                desktop.currentItem = appSettings.desktopPageCurrent
            }
        }
    }

    fun openAppDrawer() {
        openAppDrawer(null, 0, 0)
    }

    override fun openAppDrawer(view: View?, x: Int, y: Int) {
        if (!(x > 0 && y > 0) && view != null) {
            val pos = IntArray(2)
            view.getLocationInWindow(pos)
            cx = pos[0]
            cy = pos[1]

            cx += (view.width / 2f).toInt()
            cy += (view.height / 2f).toInt()

            if (view is AppItemView && view.showLabel) {
                cy -= (Tool.dp2px(14) / 2f).toInt()
            }
            cy -= appDrawerController.paddingTop
        } else {
            cx = x
            cy = y
        }
        appDrawerController.open(cx, cy)
    }

    override fun closeAppDrawer() {
        appDrawerController.close(cx, cy)
    }

    override fun openFeed() {
        // Check if we have all required permissions
        if (FeedPermissionManager.hasAllPermissions(this)) {
            // All permissions granted, open the feed
            drawerLayout.openDrawer(androidx.core.view.GravityCompat.END)
            feedView.refreshFeed()
        } else {
            // Request missing permissions
            if (feedPermissionManager.shouldShowRationale()) {
                // Show rationale dialog first
                showFeedPermissionRationale()
            } else {
                // Request permissions directly
                feedPermissionManager.requestPermissions()
            }
        }
    }

    fun closeFeed() {
        drawerLayout.closeDrawer(androidx.core.view.GravityCompat.END)
    }

    /**
     * Show a rationale dialog explaining why feed permissions are needed
     */
    private fun showFeedPermissionRationale() {
        val missingPermissions = FeedPermissionManager.getMissingPermissions(this)
        val rationaleMessage = feedPermissionManager.getRationaleMessage(missingPermissions)

        DialogHelper.alertDialog(
            this,
            "Feed Permissions",
            rationaleMessage,
            "Grant Permissions"
        ) { _, _ ->
            feedPermissionManager.requestPermissions()
        }
    }

    /**
     * Show a dialog when permissions are denied
     */
    private fun showFeedPermissionDenied(deniedPermissions: List<String>) {
        val deniedMessage = feedPermissionManager.getDeniedMessage(deniedPermissions)

        DialogHelper.alertDialog(
            this,
            "Permissions Required",
            deniedMessage,
            "OK"
        ) { _, _ ->
            // User acknowledged, do nothing
        }
    }

    companion object {
        const val REQUEST_CREATE_APPWIDGET = 0x6475
        const val REQUEST_PERMISSION_STORAGE = 0x3648
        const val REQUEST_PERMISSION_POST_NOTIFICATIONS = 0x3649
        const val REQUEST_PERMISSION_READ_MEDIA = 0x3650
        const val REQUEST_PICK_APPWIDGET = 0x2678

        @JvmStatic
        lateinit var _appWidgetHost: WidgetHost

        @JvmStatic
        lateinit var _appWidgetManager: AppWidgetManager

        @JvmStatic
        var ignoreResume = false

        @JvmStatic
        var _itemTouchX = 0f

        @JvmStatic
        var _itemTouchY = 0f

        // receiver variables
        private val _appUpdateIntentFilter = IntentFilter()
        private val _timeChangedIntentFilter = IntentFilter()

        init {
            _timeChangedIntentFilter.addAction(Intent.ACTION_TIME_TICK)
            _timeChangedIntentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED)
            _timeChangedIntentFilter.addAction(Intent.ACTION_TIME_CHANGED)
            _appUpdateIntentFilter.addDataScheme("package")
            _appUpdateIntentFilter.addAction(Intent.ACTION_PACKAGE_ADDED)
            _appUpdateIntentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED)
            _appUpdateIntentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED)
        }

    }
}
