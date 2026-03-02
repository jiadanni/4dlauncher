package com.jiadanni.launcher4d.util

import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import com.jiadanni.launcher4d.AppObject
import com.jiadanni.launcher4d.R
import com.jiadanni.launcher4d.activity.MinibarEditActivity
import com.jiadanni.launcher4d.activity.SettingsActivity
import com.jiadanni.launcher4d.viewutil.DialogHelper

object LauncherAction {

    enum class Action {
        EditMinibar, SetWallpaper, LockScreen, LauncherSettings, VolumeDialog,
        DeviceSettings, AppDrawer, SearchBar, MobileNetworkSettings,
        ShowNotifications, TurnOffScreen, Camera
    }

    @JvmField
    val actionDisplayItems = arrayOf(
        ActionDisplayItem(Action.EditMinibar, AppObject.get().resources.getString(R.string.minibar_title__edit_minibar), AppObject.get().resources.getString(R.string.minibar_summary__edit_minibar), R.drawable.ic_edit, 98),
        ActionDisplayItem(Action.SetWallpaper, AppObject.get().resources.getString(R.string.minibar_title__set_wallpaper), AppObject.get().resources.getString(R.string.minibar_summary__set_wallpaper), R.drawable.ic_photo, 36),
        ActionDisplayItem(Action.LockScreen, AppObject.get().resources.getString(R.string.minibar_title__lock_screen), AppObject.get().resources.getString(R.string.minibar_summary__lock_screen), R.drawable.ic_lock, 24),
        ActionDisplayItem(Action.LauncherSettings, AppObject.get().resources.getString(R.string.minibar_title__launcher_settings), AppObject.get().resources.getString(R.string.minibar_summary__launcher_settings), R.drawable.ic_settings, 50),
        ActionDisplayItem(Action.VolumeDialog, AppObject.get().resources.getString(R.string.minibar_title__volume_dialog), AppObject.get().resources.getString(R.string.minibar_summary__volume_dialog), R.drawable.ic_volume, 71),
        ActionDisplayItem(Action.DeviceSettings, AppObject.get().resources.getString(R.string.minibar_title__device_settings), AppObject.get().resources.getString(R.string.minibar_summary__device_settings), R.drawable.ic_android, 25),
        ActionDisplayItem(Action.AppDrawer, AppObject.get().resources.getString(R.string.minibar_title__app_drawer), AppObject.get().resources.getString(R.string.minibar_summary__app_drawer), R.drawable.ic_apps, 73),
        ActionDisplayItem(Action.SearchBar, AppObject.get().resources.getString(R.string.minibar_title__search_bar), AppObject.get().resources.getString(R.string.minibar_summary__search_bar), R.drawable.ic_search, 89),
        ActionDisplayItem(Action.MobileNetworkSettings, AppObject.get().resources.getString(R.string.minibar_title__mobile_network), AppObject.get().resources.getString(R.string.minibar_summary__mobile_network), R.drawable.ic_network, 46),
        ActionDisplayItem(Action.ShowNotifications, AppObject.get().resources.getString(R.string.minibar_title__notification_bar), AppObject.get().resources.getString(R.string.minibar_summary__notification_bar), R.drawable.ic_notifications, 46),
        ActionDisplayItem(Action.Camera, AppObject.get().resources.getString(R.string.minibar_title__camera), AppObject.get().resources.getString(R.string.minibar_summary__camera), R.drawable.ic_camera_, 13)
    )

    @JvmField
    val defaultArrangement = listOf(
        Action.EditMinibar, Action.SetWallpaper,
        Action.LockScreen, Action.LauncherSettings,
        Action.VolumeDialog, Action.DeviceSettings,
        Action.Camera
    )

    @JvmStatic
    fun runAction(action: Action, context: Context) {
        runAction(getActionItem(action)!!, context)
    }

    @Suppress("DEPRECATION")
    @JvmStatic
    fun runAction(action: ActionDisplayItem, context: Context) {
        when (action._action) {
            Action.EditMinibar -> {
                context.startActivity(Intent(context, MinibarEditActivity::class.java))
            }
            Action.SetWallpaper -> {
                context.startActivity(Intent.createChooser(
                    Intent(Intent.ACTION_SET_WALLPAPER),
                    context.getString(R.string.select_wallpaper)
                ))
            }
            Action.LockScreen -> {
                try {
                    val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
                    devicePolicyManager.lockNow()
                } catch (e: Exception) {
                    DialogHelper.alertDialog(
                        context,
                        context.getString(R.string.device_admin_title),
                        context.getString(R.string.device_admin_summary),
                        context.getString(R.string.enable)
                    ) { _, _ ->
                        Tool.toast(context, context.getString(R.string.toast_device_admin_required))
                        val intent = Intent().apply {
                            component = ComponentName("com.android.settings", "com.android.settings.DeviceAdminSettings")
                        }
                        context.startActivity(intent)
                    }
                }
            }
            Action.DeviceSettings -> {
                context.startActivity(Intent(Settings.ACTION_SETTINGS))
            }
            Action.LauncherSettings -> {
                context.startActivity(Intent(context, SettingsActivity::class.java))
            }
            Action.VolumeDialog -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    try {
                        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        audioManager.setStreamVolume(
                            AudioManager.STREAM_RING,
                            audioManager.getStreamVolume(AudioManager.STREAM_RING),
                            AudioManager.FLAG_SHOW_UI
                        )
                    } catch (e: Exception) {
                        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        if (!notificationManager.isNotificationPolicyAccessGranted) {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                            context.startActivity(intent)
                        }
                    }
                } else {
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    audioManager.setStreamVolume(
                        AudioManager.STREAM_RING,
                        audioManager.getStreamVolume(AudioManager.STREAM_RING),
                        AudioManager.FLAG_SHOW_UI
                    )
                }
            }
            Action.AppDrawer -> {
                Tool.getLauncher(context)?.openAppDrawer()
            }
            Action.SearchBar -> {
                Tool.getLauncher(context)?.searchBar?.searchButton?.performClick()
            }
            Action.MobileNetworkSettings -> {
                context.startActivity(Intent(Settings.ACTION_DATA_ROAMING_SETTINGS))
            }
            Action.ShowNotifications -> {
                try {
                    val statusBarService = context.getSystemService("statusbar")
                    val statusBarManager = Class.forName("android.app.StatusBarManager")
                    val statusBarExpand = statusBarManager.getMethod("expandNotificationsPanel")
                    statusBarExpand.invoke(statusBarService)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            Action.TurnOffScreen -> {
                try {
                    // still needs to reset screen timeout back to default on activity destroy
                    val defaultTurnOffTime = Settings.System.getInt(
                        context.contentResolver,
                        Settings.System.SCREEN_OFF_TIMEOUT,
                        60000
                    )
                    Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, 1000)
                    Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, defaultTurnOffTime)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
            }
            Action.Camera -> {
                context.startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
            }
        }
    }

    @JvmStatic
    fun getActionItem(position: Int): ActionDisplayItem {
        // used for pick action dialog
        return getActionItem(Action.values()[position])!!
    }

    @JvmStatic
    fun getActionItem(action: Action): ActionDisplayItem? {
        return getActionItem(action.toString())
    }

    @JvmStatic
    fun getActionItem(action: String): ActionDisplayItem? {
        return actionDisplayItems.find { it._action.toString() == action }
    }

    data class ActionDisplayItem(
        var _action: Action,
        var _label: String,
        var _description: String,
        var _icon: Int,
        var _id: Int
    )
}
