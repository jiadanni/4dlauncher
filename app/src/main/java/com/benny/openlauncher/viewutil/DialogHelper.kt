package com.benny.openlauncher.viewutil

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.Gravity
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.benny.openlauncher.R
import com.benny.openlauncher.activity.HomeActivity
import com.benny.openlauncher.model.App
import com.benny.openlauncher.model.Item
import com.benny.openlauncher.util.AppManager
import com.benny.openlauncher.util.AppSettings
import com.benny.openlauncher.util.Tool
import com.mikepenz.fastadapter.adapters.FastItemAdapter

object DialogHelper {

    fun editItemDialog(title: String, defaultText: String, context: Context, listener: OnItemEditListener) {
        MaterialDialog.Builder(context)
            .title(title)
            .positiveText(android.R.string.ok)
            .negativeText(android.R.string.cancel)
            .input(null, defaultText) { _, input ->
                listener.itemLabel(input.toString())
            }
            .show()
    }

    fun alertDialog(
        context: Context,
        title: String,
        msg: String,
        onPositive: MaterialDialog.SingleButtonCallback
    ) {
        MaterialDialog.Builder(context)
            .title(title)
            .onPositive(onPositive)
            .content(msg)
            .negativeText(android.R.string.cancel)
            .positiveText(android.R.string.ok)
            .show()
    }

    fun alertDialog(
        context: Context,
        title: String,
        message: String,
        positive: String,
        onPositive: MaterialDialog.SingleButtonCallback
    ) {
        MaterialDialog.Builder(context)
            .title(title)
            .onPositive(onPositive)
            .content(message)
            .negativeText(android.R.string.cancel)
            .positiveText(positive)
            .show()
    }

    fun selectActionDialog(context: Context, callback: MaterialDialog.ListCallback) {
        MaterialDialog.Builder(context)
            .title(R.string.action)
            .items(R.array.entries__gesture_action)
            .itemsCallback(callback)
            .show()
    }

    fun selectDesktopActionDialog(context: Context, callback: MaterialDialog.ListCallback) {
        MaterialDialog.Builder(context)
            .title(R.string.action)
            .items(R.array.entries__desktop_actions)
            .itemsCallback(callback)
            .show()
    }

    fun selectGestureDialog(context: Context, title: String, callback: MaterialDialog.ListCallback) {
        MaterialDialog.Builder(context)
            .title(title)
            .items(R.array.entries__gesture)
            .itemsCallback(callback)
            .show()
    }

    fun selectAppDialog(context: Context, onAppSelectedListener: OnAppSelectedListener?) {
        val fastItemAdapter = FastItemAdapter<IconLabelItem>()
        val builder = MaterialDialog.Builder(context)
            .title(R.string.select_app)
            .adapter(fastItemAdapter, LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false))
            .negativeText(android.R.string.cancel)

        val dialog = builder.build()
        val apps = AppManager.getInstance(context).apps
        val items = apps.map { app ->
            IconLabelItem(app.icon, app.label)
                .withIconSize(50)
                .withIsAppLauncher(true)
                .withIconGravity(Gravity.START)
                .withIconPadding(8)
        }

        fastItemAdapter.set(items)
        fastItemAdapter.setOnClickListener { _, _, _, position ->
            onAppSelectedListener?.onAppSelected(apps[position])
            dialog.dismiss()
            true
        }
        dialog.show()
    }

    fun startPickIconPackIntent(context: Context) {
        val packageManager = context.packageManager
        val activity = context as Activity
        val appManager = AppManager.getInstance(context)
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory("com.anddoes.launcher.THEME")
        }

        val fastItemAdapter = FastItemAdapter<IconLabelItem>()
        val resolveInfos = packageManager.queryIntentActivities(intent, 0)
            .sortedWith(compareBy({ it.loadLabel(packageManager).toString() }))

        val dialog = MaterialDialog.Builder(activity)
            .adapter(fastItemAdapter, null)
            .title(activity.getString(R.string.select_icon_pack))
            .build()

        // Add default icons option
        fastItemAdapter.add(
            IconLabelItem(activity, R.mipmap.ic_launcher, R.string.default_icons)
                .withIconPadding(16)
                .withIconGravity(Gravity.START)
                .withOnClickListener {
                    appManager._recreateAfterGettingApps = true
                    AppSettings.get().iconPack = ""
                    appManager.allApps
                    dialog.dismiss()
                }
        )

        // Add icon pack options
        resolveInfos.forEachIndexed { index, resolveInfo ->
            fastItemAdapter.add(
                IconLabelItem(
                    resolveInfo.loadIcon(packageManager),
                    resolveInfo.loadLabel(packageManager).toString()
                )
                    .withIconPadding(16)
                    .withIconSize(50)
                    .withIsAppLauncher(true)
                    .withIconGravity(Gravity.START)
                    .withOnClickListener {
                        if (ActivityCompat.checkSelfPermission(
                                context,
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            appManager._recreateAfterGettingApps = true
                            AppSettings.get().iconPack = resolveInfos[index].activityInfo.packageName
                            appManager.allApps
                            dialog.dismiss()
                        } else {
                            Tool.toast(context, activity.getString(R.string.toast_icon_pack_error))
                            ActivityCompat.requestPermissions(
                                HomeActivity.Companion.launcher,
                                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                                HomeActivity.REQUEST_PERMISSION_STORAGE
                            )
                        }
                    }
            )
        }
        dialog.show()
    }

    fun deletePackageDialog(context: Context, item: Item) {
        if (item.type == Item.Type.APP) {
            try {
                val packageUri = Uri.parse("package:${item.intent.component?.packageName}")
                val uninstallIntent = Intent(Intent.ACTION_DELETE, packageUri)
                context.startActivity(uninstallIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun interface OnAppSelectedListener {
        fun onAppSelected(app: App)
    }

    fun interface OnItemEditListener {
        fun itemLabel(label: String)
    }
}
