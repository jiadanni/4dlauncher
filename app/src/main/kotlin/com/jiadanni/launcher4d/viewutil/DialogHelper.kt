package com.jiadanni.launcher4d.viewutil

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.jiadanni.launcher4d.R
import com.jiadanni.launcher4d.activity.HomeActivity
import com.jiadanni.launcher4d.model.App
import com.jiadanni.launcher4d.model.Item
import com.jiadanni.launcher4d.util.AppManager
import com.jiadanni.launcher4d.util.AppSettings
import com.jiadanni.launcher4d.util.Tool
import com.mikepenz.fastadapter.adapters.FastItemAdapter
import java.util.*

object DialogHelper {
    fun editItemDialog(title: String, defaultText: String, c: Context, listener: OnItemEditListener) {
        val input = EditText(c)
        input.setText(defaultText)
        val builder = AlertDialog.Builder(c)
        builder.setTitle(title)
                .setView(input)
                .setPositiveButton(android.R.string.ok) { _, _ -> listener.itemLabel(input.text.toString()) }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
    }

    fun alertDialog(context: Context, title: String, msg: String, onPositive: DialogInterface.OnClickListener? = null) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
                .setMessage(msg)
                .setPositiveButton(android.R.string.ok, onPositive)
                .setNegativeButton(android.R.string.cancel, null)
                .show()
    }

    fun alertDialog(context: Context, title: String, message: String, positive: String, onPositive: DialogInterface.OnClickListener? = null) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton(positive, onPositive)
                .setNegativeButton(android.R.string.cancel, null)
                .show()
    }

    fun selectActionDialog(context: Context, callback: DialogInterface.OnClickListener) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.action)
                .setItems(R.array.entries__gesture_action, callback)
                .show()
    }

    fun selectDesktopActionDialog(context: Context, callback: DialogInterface.OnClickListener) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(R.string.action)
                .setItems(R.array.entries__desktop_actions, callback)
                .show()
    }

    fun selectGestureDialog(context: Context, title: String, callback: DialogInterface.OnClickListener) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
                .setItems(R.array.entries__gesture, callback)
                .show()
    }

    fun selectAppDialog(context: Context, onAppSelectedListener: OnAppSelectedListener?) {
        val builder = AlertDialog.Builder(context)
        val fastItemAdapter = FastItemAdapter<IconLabelItem>()
        builder.setTitle(R.string.select_app)
                .setNegativeButton(android.R.string.cancel, null)

        val apps = AppManager.getInstance(context).nonFilteredApps
        val items = ArrayList<IconLabelItem>()
        for (app in apps) {
            items.add(IconLabelItem(app.icon, app.label)
                    .withIconSize(50)
                    .withIsAppLauncher(true)
                    .withIconGravity(Gravity.START)
                    .withIconPadding(8))
        }
        fastItemAdapter.set(items)
        
        val dialog = builder.create()
        fastItemAdapter.onClickListener = { _, _, _, position ->
            onAppSelectedListener?.onAppSelected(apps[position])
            dialog.dismiss()
            true
        }
        
        // We need a view for the adapter
        val recyclerView = androidx.recyclerview.widget.RecyclerView(context)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = fastItemAdapter
        builder.setView(recyclerView)
        builder.show()
    }

    fun startPickIconPackIntent(context: Context) {
        val packageManager = context.packageManager
        val activity = context as Activity
        val appManager = AppManager.getInstance(context)
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory("com.anddoes.launcher.THEME")

        val fastItemAdapter = FastItemAdapter<IconLabelItem>()

        val resolveInfos = packageManager.queryIntentActivities(intent, 0)
        Collections.sort(resolveInfos, ResolveInfo.DisplayNameComparator(packageManager))
        
        val builder = AlertDialog.Builder(activity)
        builder.setTitle(R.string.select_icon_pack)

        fastItemAdapter.add(IconLabelItem(activity, R.mipmap.ic_launcher, R.string.default_icons)
                .withIconPadding(16)
                .withIconGravity(Gravity.START)
                .withOnClickListener {
                    appManager._recreateAfterGettingApps = true
                    AppSettings.get().iconPack = ""
                    appManager.getAllApps()
                })

        for (info in resolveInfos) {
            fastItemAdapter.add(IconLabelItem(info.loadIcon(packageManager), info.loadLabel(packageManager).toString())
                    .withIconPadding(16)
                    .withIconSize(50)
                    .withIsAppLauncher(true)
                    .withIconGravity(Gravity.START)
                    .withOnClickListener {
                        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                            appManager._recreateAfterGettingApps = true
                            AppSettings.get().iconPack = info.activityInfo.packageName
                            appManager.getAllApps()
                        } else {
                            Tool.toast(context, activity.getString(R.string.toast_icon_pack_error))
                            ActivityCompat.requestPermissions(HomeActivity.launcher!!, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), HomeActivity.REQUEST_PERMISSION_STORAGE)
                        }
                    })
        }

        val recyclerView = androidx.recyclerview.widget.RecyclerView(context)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = fastItemAdapter
        builder.setView(recyclerView)
        builder.show()
    }

    fun deletePackageDialog(context: Context, item: Item) {
        if (item.type == Item.Type.APP) {
            try {
                val packageURI = Uri.parse("package:" + item.intent?.component?.packageName)
                val uninstallIntent = Intent(Intent.ACTION_DELETE, packageURI)
                context.startActivity(uninstallIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    interface OnAppSelectedListener {
        fun onAppSelected(app: App)
    }

    interface OnItemEditListener {
        fun itemLabel(label: String)
    }
}
