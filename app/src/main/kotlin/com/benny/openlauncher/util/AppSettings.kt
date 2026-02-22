package com.benny.openlauncher.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.core.content.ContextCompat
import com.benny.openlauncher.AppObject
import com.benny.openlauncher.R
import com.benny.openlauncher.manager.Setup
import com.benny.openlauncher.widget.AppDrawerController
import com.benny.openlauncher.widget.PagerIndicator
import net.gsantner.opoc.preference.SharedPreferencesPropertyBackend
import org.threeten.bp.format.DateTimeFormatter

class AppSettings(context: Context) : SharedPreferencesPropertyBackend(context, "app") {

    val desktopColumnCount: Int
        get() = getInt(R.string.pref_key__desktop_columns, 5)

    val desktopRowCount: Int
        get() = getInt(R.string.pref_key__desktop_rows, 6)

    val desktopIndicatorMode: Int
        get() = getIntOfStringPref(R.string.pref_key__desktop_indicator_style, PagerIndicator.Mode.DOTS)

    val desktopOrientationMode: Int
        get() = getIntOfStringPref(R.string.pref_key__desktop_orientation, 0)

    val desktopWallpaperScroll: Definitions.WallpaperScroll
        get() {
            return when (getIntOfStringPref(R.string.pref_key__desktop_wallpaper_scroll, 0)) {
                1 -> Definitions.WallpaperScroll.Inverse
                2 -> Definitions.WallpaperScroll.Off
                else -> Definitions.WallpaperScroll.Normal
            }
        }

    val desktopShowGrid: Boolean
        get() = getBool(R.string.pref_key__desktop_show_grid, true)

    val desktopFullscreen: Boolean
        get() = getBool(R.string.pref_key__desktop_fullscreen, false)

    val desktopShowIndicator: Boolean
        get() = getBool(R.string.pref_key__desktop_show_position_indicator, true)

    val desktopShowLabel: Boolean
        get() = getBool(R.string.pref_key__desktop_show_label, true)

    val searchBarEnable: Boolean
        get() = getBool(R.string.pref_key__search_bar_enable, true)

    val searchBarStartsWith: Boolean
        get() = getBool(R.string.pref_key__search_bar_starts_with, true)

    val searchBarBaseURI: String
        get() = getString(R.string.pref_key__search_bar_base_uri, R.string.pref_default__search_bar_base_uri)

    val searchBarForceBrowser: Boolean
        get() = getBool(R.string.pref_key__search_bar_force_browser, false)

    val searchBarShouldShowHiddenApps: Boolean
        get() = getBool(R.string.pref_key__search_bar_show_hidden_apps, false)

    val userDateFormat: DateTimeFormatter
        get() {
            val line1 = getString(R.string.pref_key__date_bar_date_format_custom_1, rstr(R.string.pref_default__date_bar_date_format_custom_1))
            val line2 = getString(R.string.pref_key__date_bar_date_format_custom_2, rstr(R.string.pref_default__date_bar_date_format_custom_2))
            return DateTimeFormatter.ofPattern("$line1'\n'$line2")
        }

    val desktopDateMode: Int
        get() = getIntOfStringPref(R.string.pref_key__date_bar_date_format_type, 1)

    val desktopDateTextColor: Int
        get() = getInt(R.string.pref_key__date_bar_date_text_color, Color.WHITE)

    val desktopBackgroundColor: Int
        get() = getInt(R.string.pref_key__desktop_background_color, Color.TRANSPARENT)

    val desktopInsetColor: Int
        get() = getInt(R.string.pref_key__desktop_inset_color, Color.TRANSPARENT)

    val desktopFolderColor: Int
        get() = getInt(R.string.pref_key__desktop_folder_color, Color.WHITE)

    val minibarBackgroundColor: Int
        get() = getInt(R.string.pref_key__minibar_background_color, ContextCompat.getColor(_context, R.color.colorPrimary))

    val desktopIconSize: Int
        get() = iconSize

    val dockEnable: Boolean
        get() = getBool(R.string.pref_key__dock_enable, true)

    val dockColumnCount: Int
        get() = getInt(R.string.pref_key__dock_columns, 5)

    val dockRowCount: Int
        get() = getInt(R.string.pref_key__dock_rows, 1)

    val dockShowLabel: Boolean
        get() = getBool(R.string.pref_key__dock_show_label, false)

    val dockColor: Int
        get() = getInt(R.string.pref_key__dock_background_color, Color.TRANSPARENT)

    val dockIconSize: Int
        get() = iconSize

    val drawerColumnCount: Int
        get() = getInt(R.string.pref_key__drawer_columns, 5)

    val drawerRowCount: Int
        get() = getInt(R.string.pref_key__drawer_rows, 6)

    val drawerStyle: Int
        get() = getIntOfStringPref(R.string.pref_key__drawer_style, AppDrawerController.Mode.GRID)

    val drawerShowCardView: Boolean
        get() = getBool(R.string.pref_key__drawer_show_card_view, true)

    val drawerRememberPosition: Boolean
        get() = getBool(R.string.pref_key__drawer_remember_position, true)

    val drawerShowIndicator: Boolean
        get() = getBool(R.string.pref_key__drawer_show_position_indicator, true)

    val drawerShowLabel: Boolean
        get() = getBool(R.string.pref_key__drawer_show_label, true)

    val drawerBackgroundColor: Int
        get() = getInt(R.string.pref_key__drawer_background_color, rcolor(R.color.shade))

    val drawerCardColor: Int
        get() = getInt(R.string.pref_key__drawer_card_color, rcolor(R.color.shade))

    val drawerLabelColor: Int
        get() = getInt(R.string.pref_key__drawer_label_color, Color.WHITE)

    val drawerFastScrollColor: Int
        get() = getInt(R.string.pref_key__drawer_fast_scroll_color, ContextCompat.getColor(Setup.appContext(), R.color.materialRed))

    val gestureFeedback: Boolean
        get() = getBool(R.string.pref_key__gesture_feedback, false)

    val gestureDockSwipeUp: Boolean
        get() = getBool(R.string.pref_key__gesture_quick_swipe, true)

    val gestureDoubleTap: Any?
        get() = getGesture(R.string.pref_key__gesture_double_tap)

    val gestureSwipeUp: Any?
        get() = getGesture(R.string.pref_key__gesture_swipe_up)

    val gestureSwipeDown: Any?
        get() = getGesture(R.string.pref_key__gesture_swipe_down)

    val gesturePinch: Any?
        get() = getGesture(R.string.pref_key__gesture_pinch_in)

    val gestureUnpinch: Any?
        get() = getGesture(R.string.pref_key__gesture_pinch_out)

    val theme: String
        get() = getString(R.string.pref_key__theme, "1")

    val primaryColor: Int
        get() = getInt(R.string.pref_key__primary_color, _context.resources.getColor(R.color.colorPrimary))

    val iconSize: Int
        get() = getInt(R.string.pref_key__icon_size, 52)

    val iconPack: String
        get() = getString(R.string.pref_key__icon_pack, "")

    val notificationStatus: Boolean
        get() = getBool(R.string.pref_key__gesture_notifications, false)

    val animationSpeed: Int
        get() = 100 - getInt(R.string.pref_key__animation_speed, 80)

    val language: String
        get() = getString(R.string.pref_key__language, "")

    var minibarEnable: Boolean
        get() = getBool(R.string.pref_key__minibar_enable, true)
        set(value) = setBool(R.string.pref_key__minibar_enable, value)

    var searchUseGrid: Boolean
        get() = getBool(R.string.pref_key__desktop_search_use_grid, false)
        set(value) = setBool(R.string.pref_key__desktop_search_use_grid, value)

    var hiddenAppsList: ArrayList<String>?
        get() = getStringList(R.string.pref_key__hidden_apps)
        set(value) = setStringList(R.string.pref_key__hidden_apps, value)

    var desktopPageCurrent: Int
        get() = getInt(R.string.pref_key__desktop_current_position, 0)
        set(value) = setInt(R.string.pref_key__desktop_current_position, value)

    var desktopLock: Boolean
        get() = getBool(R.string.pref_key__desktop_lock, false)
        set(value) = setBool(R.string.pref_key__desktop_lock, value)

    var appRestartRequired: Boolean
        get() = getBool(R.string.pref_key__queue_restart, false)
        @SuppressLint("ApplySharedPref")
        set(value) {
            // MUST be committed
            _prefApp.edit().putBoolean(_context.getString(R.string.pref_key__queue_restart), value).commit()
        }

    var appFirstLaunch: Boolean
        get() = getBool(R.string.pref_key__first_start, true)
        @SuppressLint("ApplySharedPref")
        set(value) {
            // MUST be committed
            _prefApp.edit().putBoolean(_context.getString(R.string.pref_key__first_start), value).commit()
        }

    fun getGesture(key: Int): Any? {
        // return either ActionItem or Intent
        val result = getString(key, "")
        var gesture: Any? = LauncherAction.getActionItem(result)
        // no action was found so it must be an intent string
        if (gesture == null) {
            gesture = Tool.getIntentFromString(result)
            if (AppManager.getInstance(_context).findApp(gesture as? Intent) == null) {
                gesture = null
            }
        }
        // reset the setting if invalid value
        if (gesture == null) {
            setString(key, null)
        }
        return gesture
    }

    fun setIconPack(value: String) {
        setString(R.string.pref_key__icon_pack, value)
    }

    fun getMinibarArrangement(): ArrayList<LauncherAction.ActionDisplayItem> {
        val minibarString = getStringList(R.string.pref_key__minibar_items)
        val minibarObject = ArrayList<LauncherAction.ActionDisplayItem>()
        for (action in minibarString) {
            val item = LauncherAction.getActionItem(action)
            if (item != null) {
                minibarObject.add(item)
            }
        }
        if (minibarObject.isEmpty()) {
            for (item in LauncherAction.actionDisplayItems) {
                if (LauncherAction.defaultArrangement.contains(item._action)) {
                    minibarObject.add(item)
                }
            }
            setMinibarArrangement(minibarString)
        }
        return minibarObject
    }

    fun setMinibarArrangement(value: ArrayList<String>) {
        setStringList(R.string.pref_key__minibar_items, value)
    }

    @SuppressLint("ApplySharedPref")
    fun setAppShowIntro(value: Boolean) {
        // MUST be committed
        _prefApp.edit().putBoolean(_context.getString(R.string.pref_key__show_intro), value).commit()
    }

    // Deprecated Java-style getters for backward compatibility
    @Deprecated("Use desktopColumnCount property", ReplaceWith("desktopColumnCount"))
    fun getDesktopColumnCount(): Int = desktopColumnCount

    @Deprecated("Use desktopRowCount property", ReplaceWith("desktopRowCount"))
    fun getDesktopRowCount(): Int = desktopRowCount

    @Deprecated("Use desktopIndicatorMode property", ReplaceWith("desktopIndicatorMode"))
    fun getDesktopIndicatorMode(): Int = desktopIndicatorMode

    @Deprecated("Use desktopOrientationMode property", ReplaceWith("desktopOrientationMode"))
    fun getDesktopOrientationMode(): Int = desktopOrientationMode

    @Deprecated("Use desktopWallpaperScroll property", ReplaceWith("desktopWallpaperScroll"))
    fun getDesktopWallpaperScroll(): Definitions.WallpaperScroll = desktopWallpaperScroll

    @Deprecated("Use desktopShowGrid property", ReplaceWith("desktopShowGrid"))
    fun getDesktopShowGrid(): Boolean = desktopShowGrid

    @Deprecated("Use desktopFullscreen property", ReplaceWith("desktopFullscreen"))
    fun getDesktopFullscreen(): Boolean = desktopFullscreen

    @Deprecated("Use desktopShowIndicator property", ReplaceWith("desktopShowIndicator"))
    fun getDesktopShowIndicator(): Boolean = desktopShowIndicator

    @Deprecated("Use desktopShowLabel property", ReplaceWith("desktopShowLabel"))
    fun getDesktopShowLabel(): Boolean = desktopShowLabel

    @Deprecated("Use searchBarEnable property", ReplaceWith("searchBarEnable"))
    fun getSearchBarEnable(): Boolean = searchBarEnable

    @Deprecated("Use searchBarStartsWith property", ReplaceWith("searchBarStartsWith"))
    fun getSearchBarStartsWith(): Boolean = searchBarStartsWith

    @Deprecated("Use searchBarBaseURI property", ReplaceWith("searchBarBaseURI"))
    fun getSearchBarBaseURI(): String = searchBarBaseURI

    @Deprecated("Use searchBarForceBrowser property", ReplaceWith("searchBarForceBrowser"))
    fun getSearchBarForceBrowser(): Boolean = searchBarForceBrowser

    @Deprecated("Use searchBarShouldShowHiddenApps property", ReplaceWith("searchBarShouldShowHiddenApps"))
    fun getSearchBarShouldShowHiddenApps(): Boolean = searchBarShouldShowHiddenApps

    @Deprecated("Use userDateFormat property", ReplaceWith("userDateFormat"))
    fun getUserDateFormat(): DateTimeFormatter = userDateFormat

    @Deprecated("Use desktopDateMode property", ReplaceWith("desktopDateMode"))
    fun getDesktopDateMode(): Int = desktopDateMode

    @Deprecated("Use desktopDateTextColor property", ReplaceWith("desktopDateTextColor"))
    fun getDesktopDateTextColor(): Int = desktopDateTextColor

    @Deprecated("Use desktopBackgroundColor property", ReplaceWith("desktopBackgroundColor"))
    fun getDesktopBackgroundColor(): Int = desktopBackgroundColor

    @Deprecated("Use desktopInsetColor property", ReplaceWith("desktopInsetColor"))
    fun getDesktopInsetColor(): Int = desktopInsetColor

    @Deprecated("Use desktopFolderColor property", ReplaceWith("desktopFolderColor"))
    fun getDesktopFolderColor(): Int = desktopFolderColor

    @Deprecated("Use minibarBackgroundColor property", ReplaceWith("minibarBackgroundColor"))
    fun getMinibarBackgroundColor(): Int = minibarBackgroundColor

    @Deprecated("Use desktopIconSize property", ReplaceWith("desktopIconSize"))
    fun getDesktopIconSize(): Int = desktopIconSize

    @Deprecated("Use dockEnable property", ReplaceWith("dockEnable"))
    fun getDockEnable(): Boolean = dockEnable

    @Deprecated("Use dockColumnCount property", ReplaceWith("dockColumnCount"))
    fun getDockColumnCount(): Int = dockColumnCount

    @Deprecated("Use dockRowCount property", ReplaceWith("dockRowCount"))
    fun getDockRowCount(): Int = dockRowCount

    @Deprecated("Use dockShowLabel property", ReplaceWith("dockShowLabel"))
    fun getDockShowLabel(): Boolean = dockShowLabel

    @Deprecated("Use dockColor property", ReplaceWith("dockColor"))
    fun getDockColor(): Int = dockColor

    @Deprecated("Use dockIconSize property", ReplaceWith("dockIconSize"))
    fun getDockIconSize(): Int = dockIconSize

    @Deprecated("Use drawerColumnCount property", ReplaceWith("drawerColumnCount"))
    fun getDrawerColumnCount(): Int = drawerColumnCount

    @Deprecated("Use drawerRowCount property", ReplaceWith("drawerRowCount"))
    fun getDrawerRowCount(): Int = drawerRowCount

    @Deprecated("Use drawerStyle property", ReplaceWith("drawerStyle"))
    fun getDrawerStyle(): Int = drawerStyle

    @Deprecated("Use drawerShowCardView property", ReplaceWith("drawerShowCardView"))
    fun getDrawerShowCardView(): Boolean = drawerShowCardView

    @Deprecated("Use drawerRememberPosition property", ReplaceWith("drawerRememberPosition"))
    fun getDrawerRememberPosition(): Boolean = drawerRememberPosition

    @Deprecated("Use drawerShowIndicator property", ReplaceWith("drawerShowIndicator"))
    fun getDrawerShowIndicator(): Boolean = drawerShowIndicator

    @Deprecated("Use drawerShowLabel property", ReplaceWith("drawerShowLabel"))
    fun getDrawerShowLabel(): Boolean = drawerShowLabel

    @Deprecated("Use drawerBackgroundColor property", ReplaceWith("drawerBackgroundColor"))
    fun getDrawerBackgroundColor(): Int = drawerBackgroundColor

    @Deprecated("Use drawerCardColor property", ReplaceWith("drawerCardColor"))
    fun getDrawerCardColor(): Int = drawerCardColor

    @Deprecated("Use drawerLabelColor property", ReplaceWith("drawerLabelColor"))
    fun getDrawerLabelColor(): Int = drawerLabelColor

    @Deprecated("Use drawerFastScrollColor property", ReplaceWith("drawerFastScrollColor"))
    fun getDrawerFastScrollColor(): Int = drawerFastScrollColor

    @Deprecated("Use gestureFeedback property", ReplaceWith("gestureFeedback"))
    fun getGestureFeedback(): Boolean = gestureFeedback

    @Deprecated("Use gestureDockSwipeUp property", ReplaceWith("gestureDockSwipeUp"))
    fun getGestureDockSwipeUp(): Boolean = gestureDockSwipeUp

    @Deprecated("Use gestureDoubleTap property", ReplaceWith("gestureDoubleTap"))
    fun getGestureDoubleTap(): Any? = gestureDoubleTap

    @Deprecated("Use gestureSwipeUp property", ReplaceWith("gestureSwipeUp"))
    fun getGestureSwipeUp(): Any? = gestureSwipeUp

    @Deprecated("Use gestureSwipeDown property", ReplaceWith("gestureSwipeDown"))
    fun getGestureSwipeDown(): Any? = gestureSwipeDown

    @Deprecated("Use gesturePinch property", ReplaceWith("gesturePinch"))
    fun getGesturePinch(): Any? = gesturePinch

    @Deprecated("Use gestureUnpinch property", ReplaceWith("gestureUnpinch"))
    fun getGestureUnpinch(): Any? = gestureUnpinch

    @Deprecated("Use theme property", ReplaceWith("theme"))
    fun getTheme(): String = theme

    @Deprecated("Use primaryColor property", ReplaceWith("primaryColor"))
    fun getPrimaryColor(): Int = primaryColor

    @Deprecated("Use iconSize property", ReplaceWith("iconSize"))
    fun getIconSize(): Int = iconSize

    @Deprecated("Use iconPack property", ReplaceWith("iconPack"))
    fun getIconPack(): String = iconPack

    @Deprecated("Use notificationStatus property", ReplaceWith("notificationStatus"))
    fun getNotificationStatus(): Boolean = notificationStatus

    @Deprecated("Use animationSpeed property", ReplaceWith("animationSpeed"))
    fun getAnimationSpeed(): Int = animationSpeed

    @Deprecated("Use language property", ReplaceWith("language"))
    fun getLanguage(): String = language

    @Deprecated("Use minibarEnable property", ReplaceWith("minibarEnable"))
    fun getMinibarEnable(): Boolean = minibarEnable

    @Deprecated("Use minibarEnable property", ReplaceWith("minibarEnable = value"))
    fun setMinibarEnable(value: Boolean) {
        minibarEnable = value
    }

    @Deprecated("Use searchUseGrid property", ReplaceWith("searchUseGrid"))
    fun getSearchUseGrid(): Boolean = searchUseGrid

    // Feed settings
    val feedWeatherApiKey: String
        get() = getString(R.string.pref_key__feed_weather_api_key, R.string.pref_default__feed_weather_api_key)

    @Deprecated("Use searchUseGrid property", ReplaceWith("searchUseGrid = enabled"))
    fun setSearchUseGrid(enabled: Boolean) {
        searchUseGrid = enabled
    }

    @Deprecated("Use hiddenAppsList property", ReplaceWith("hiddenAppsList"))
    fun getHiddenAppsList(): ArrayList<String>? = hiddenAppsList

    @Deprecated("Use hiddenAppsList property", ReplaceWith("hiddenAppsList = value"))
    fun setHiddenAppsList(value: ArrayList<String>) {
        hiddenAppsList = value
    }

    @Deprecated("Use desktopPageCurrent property", ReplaceWith("desktopPageCurrent"))
    fun getDesktopPageCurrent(): Int = desktopPageCurrent

    @Deprecated("Use desktopPageCurrent property", ReplaceWith("desktopPageCurrent = value"))
    fun setDesktopPageCurrent(value: Int) {
        desktopPageCurrent = value
    }

    @Deprecated("Use desktopLock property", ReplaceWith("desktopLock"))
    fun getDesktopLock(): Boolean = desktopLock

    @Deprecated("Use desktopLock property", ReplaceWith("desktopLock = value"))
    fun setDesktopLock(value: Boolean) {
        desktopLock = value
    }

    @Deprecated("Use appRestartRequired property", ReplaceWith("appRestartRequired"))
    fun getAppRestartRequired(): Boolean = appRestartRequired

    @Deprecated("Use appRestartRequired property", ReplaceWith("appRestartRequired = value"))
    fun setAppRestartRequired(value: Boolean) {
        appRestartRequired = value
    }

    @Deprecated("Use appFirstLaunch property", ReplaceWith("appFirstLaunch"))
    fun getAppFirstLaunch(): Boolean = appFirstLaunch

    @Deprecated("Use appFirstLaunch property", ReplaceWith("appFirstLaunch = value"))
    fun setAppFirstLaunch(value: Boolean) {
        appFirstLaunch = value
    }

    companion object {
        @JvmStatic
        fun get(): AppSettings {
            return AppSettings(AppObject.get())
        }
    }
}
