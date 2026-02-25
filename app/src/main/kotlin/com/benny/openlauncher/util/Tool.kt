package com.benny.openlauncher.util

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.ShortcutQuery.*
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.benny.openlauncher.activity.HomeActivity
import com.benny.openlauncher.interfaces.Launcher
import com.benny.openlauncher.manager.Setup
import com.benny.openlauncher.model.App
import java.io.File
import java.io.FileOutputStream
import kotlin.math.ceil
import android.graphics.BitmapFactory

object Tool {

    fun hideKeyboard(context: Context, view: View) {
        val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            ?: return
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
    }

    fun showKeyboard(context: Context, view: View) {
        val inputMethodManager = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            ?: return
        inputMethodManager.toggleSoftInputFromWindow(
            view.windowToken,
            InputMethodManager.SHOW_IMPLICIT,
            InputMethodManager.HIDE_NOT_ALWAYS
        )
    }

    fun vibrate(view: View) {
        val vibrator = view.context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (vibrator == null) {
            // some manufacturers do not vibrate on long press
            // might as well make this a fallback method
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, 160))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }

    fun visibleViews(duration: Long, vararg views: View?) {
        views.filterNotNull().forEach { view ->
            view.animate()
                .alpha(1f)
                .setDuration(duration)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withStartAction { view.visibility = View.VISIBLE }
        }
    }

    fun invisibleViews(duration: Long, vararg views: View?) {
        views.filterNotNull().forEach { view ->
            view.animate()
                .alpha(0f)
                .setDuration(duration)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction { view.visibility = View.INVISIBLE }
        }
    }

    fun goneViews(duration: Long, vararg views: View?) {
        views.filterNotNull().forEach { view ->
            view.animate()
                .alpha(0f)
                .setDuration(duration)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .withEndAction { view.visibility = View.GONE }
        }
    }

    fun createScaleInScaleOutAnim(view: View, action: Runnable) {
        val animTime = (Setup.appSettings().animationSpeed * 4).toLong()
        val handler = Handler(Looper.getMainLooper())

        view.animate()
            .scaleX(0.85f)
            .scaleY(0.85f)
            .setDuration(animTime)
            .setInterpolator(AccelerateDecelerateInterpolator())

        handler.postDelayed({
            view.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(animTime)
                .setInterpolator(AccelerateDecelerateInterpolator())

            handler.postDelayed({ action.run() }, animTime)
        }, animTime)
    }

    fun toast(context: Context, str: Int) {
        Toast.makeText(context, context.resources.getString(str), Toast.LENGTH_SHORT).show()
    }

    fun toast(context: Context, str: String) {
        Toast.makeText(context, str, Toast.LENGTH_SHORT).show()
    }

    fun isPackageInstalled(packageName: String, packageManager: PackageManager): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun dp2px(dp: Float): Int {
        val resources = Resources.getSystem()
        val px = dp * resources.displayMetrics.density
        return ceil(px).toInt()
    }

    fun sp2px(sp: Float): Int {
        val resources = Resources.getSystem()
        val px = sp * resources.displayMetrics.scaledDensity
        return ceil(px).toInt()
    }

    fun clampInt(target: Int, min: Int, max: Int): Int {
        return target.coerceIn(min, max)
    }

    fun clampFloat(target: Float, min: Float, max: Float): Float {
        return target.coerceIn(min, max)
    }

    fun getLauncher(context: Context?): Launcher? {
        if (context == null) return null
        if (context is Launcher) return context
        if (context is android.content.ContextWrapper) return getLauncher(context.baseContext)
        return null
    }

    fun startApp(context: Context, app: App, view: View?) {
        val launcher = getLauncher(context)
        if (launcher is HomeActivity) {
            launcher.onStartApp(context, app, view)
        }
    }

    fun drawableToBitmap(drawable: Drawable?): Bitmap? {
        if (drawable == null) return null

        if (drawable is BitmapDrawable) {
            drawable.bitmap?.let { return it }
        }

        val bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            // single color bitmap will be created
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        } else {
            Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
        }

        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        drawable.draw(canvas)

        return bitmap
    }

    fun convertPoint(fromPoint: Point, fromView: View, toView: View): Point {
        val fromCoordinate = IntArray(2)
        val toCoordinate = IntArray(2)
        fromView.getLocationOnScreen(fromCoordinate)
        toView.getLocationOnScreen(toCoordinate)

        return Point(
            fromCoordinate[0] - toCoordinate[0] + fromPoint.x,
            fromCoordinate[1] - toCoordinate[1] + fromPoint.y
        )
    }

    fun isIntentActionAvailable(context: Context, action: String): Boolean {
        val packageManager = context.packageManager
        val intent = Intent(action)
        val resolveInfo = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo.isNotEmpty()
    }

    fun getIntentAsString(intent: Intent?): String {
        return intent?.toUri(0) ?: ""
    }

    fun getIntentFromString(string: String): Intent? {
        return try {
            Intent.parseUri(string, 0)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getIntentFromApp(app: App): Intent {
        return Intent(Intent.ACTION_MAIN).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            setClassName(app.packageName, app.className)
        }
    }

    fun getIcon(context: Context, filename: String): Drawable? {
        val bitmap = BitmapFactory.decodeFile("${context.filesDir}/icons/$filename.png")
        return bitmap?.let { BitmapDrawable(context.resources, it) }
    }

    fun saveIcon(context: Context, icon: Bitmap, filename: String) {
        val directory = File(context.filesDir, "icons")
        if (!directory.exists()) directory.mkdir()

        val file = File(directory, "$filename.png")
        try {
            file.createNewFile()
            FileOutputStream(file).use { out ->
                icon.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun removeIcon(context: Context, filename: String) {
        val file = File("${context.filesDir}/icons/$filename.png")
        if (file.exists()) {
            try {
                file.delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getShortcutInfo(context: Context, packageName: String): List<ShortcutInfo>? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N_MR1) return null

        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            ?: return null

        val shortcutQuery = LauncherApps.ShortcutQuery().apply {
            setQueryFlags(FLAG_MATCH_DYNAMIC or FLAG_MATCH_MANIFEST or FLAG_MATCH_PINNED)
            setPackage(packageName)
        }

        return try {
            launcherApps.getShortcuts(shortcutQuery, Process.myUserHandle())
        } catch (e: SecurityException) {
            Log.w(Tool::class.java.simpleName, "Can't get shortcuts info. App is not set as default launcher")
            null
        }
    }
}
