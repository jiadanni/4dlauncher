package com.benny.openlauncher.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import com.benny.openlauncher.R
import com.benny.openlauncher.activity.HomeActivity
import com.benny.openlauncher.activity.homeparts.HpInitSetup
import com.benny.openlauncher.manager.Setup
import com.benny.openlauncher.model.Item
import com.benny.openlauncher.util.Definitions
import com.benny.openlauncher.util.Tool

class ShortcutReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val extras = intent.extras ?: return

        // this will only work before Android Oreo
        // was deprecated in favor of ShortcutManager.pinRequestShortcut()
        val shortcutLabel = extras.getString(Intent.EXTRA_SHORTCUT_NAME)
        val shortcutIntent = extras.get(Intent.EXTRA_SHORTCUT_INTENT) as? Intent
        var shortcutIcon: Drawable? = null

        try {
            val parcelable = extras.getParcelable<Intent.ShortcutIconResource>(Intent.EXTRA_SHORTCUT_ICON_RESOURCE)
            if (parcelable != null) {
                val resources = context.packageManager.getResourcesForApplication(parcelable.packageName)
                if (resources != null) {
                    val id = resources.getIdentifier(parcelable.resourceName, null, null)
                    shortcutIcon = resources.getDrawable(id, null)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (shortcutIcon == null) {
                val bitmap = extras.getParcelable<Bitmap>(Intent.EXTRA_SHORTCUT_ICON)
                shortcutIcon = BitmapDrawable(context.resources, bitmap)
            }
        }

        if (!Setup.wasInitialised()) {
            Setup.init(HpInitSetup(context))
        }

        val app = Setup.appLoader().createApp(shortcutIntent!!)
        val item = if (app != null) {
            Item.newAppItem(app)
        } else {
            Item.newShortcutItem(shortcutIntent, shortcutIcon, shortcutLabel)
        }

        val launcher = HomeActivity.launcher
        val desktop = launcher?.desktop
        val currentPage = desktop?.currentItem ?: 0
        val preferredPos = desktop?.pages?.get(currentPage)?.findFreeSpace()

        if (preferredPos == null) {
            launcher?.let { Tool.toast(it, R.string.toast_not_enough_space) }
        } else {
            item.x = preferredPos.x
            item.y = preferredPos.y
            HomeActivity._db.saveItem(item, currentPage, Definitions.ItemPosition.Desktop)
            desktop?.addItemToPage(item, currentPage)
            Log.d(this::class.java.toString(), "shortcut installed")
        }
    }
}
