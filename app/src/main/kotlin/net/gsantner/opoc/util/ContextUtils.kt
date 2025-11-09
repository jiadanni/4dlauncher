/*#######################################################
 * 
 *   Maintained 2016-2023 by Gregor Santner <gsantner @ mailbox . org>
 * 
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *     https://github.com/gsantner/opoc/#licensing
 *
#########################################################*/
package net.gsantner.opoc.util

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.media.MediaScannerConnection
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Html
import android.text.InputFilter
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.util.DisplayMetrics
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.webkit.MimeTypeMap
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.core.app.ActivityManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.text.TextUtilsCompat
import androidx.core.util.Pair
import androidx.core.view.ViewCompat
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import net.gsantner.opoc.format.markdown.SimpleMarkdownParser
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.lang.reflect.Field
import java.text.SimpleDateFormat
import java.util.Locale

@Suppress(
    "WeakerAccess",
    "unused",
    "SameParameterValue",
    "ObsoleteSdkInt",
    "deprecation",
    "SpellCheckingInspection",
    "TryFinallyCanBeTryWithResources",
    "UnusedAssignment",
    "UnusedReturnValue"
)
open class ContextUtils(protected var _context: Context?) {

    fun context(): Context? {
        return _context
    }

    open fun freeContextRef() {
        _context = null
    }

    //
    // Class Methods
    //
    enum class ResType { 
        ID, BOOL, INTEGER, COLOR, STRING, ARRAY, DRAWABLE, PLURALS,
        ANIM, ATTR, DIMEN, LAYOUT, MENU, RAW, STYLE, XML
    }

    /**
     * Find out the nuermical ressource id by given [ResType]
     *
     * @return A valid id if the id could be found, else 0
     */
    fun getResId(resType: ResType, name: String): Int {
        try {
            return _context!!.resources.getIdentifier(name, resType.name.toLowerCase(), _context!!.packageName)
        } catch (e: Exception) {
            return 0
        }
    }

    /**
     * Get String by given string ressource id (nuermic)
     */
    fun rstr(@StringRes strResId: Int): String? {
        try {
            return _context!!.getString(strResId)
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Get String by given string ressource identifier (textual)
     */
    fun rstr(strResKey: String, vararg a0getResKeyAsFallback: Any): String? {
        try {
            return rstr(getResId(ResType.STRING, strResKey))
        } catch (e: Resources.NotFoundException) {
            return if (a0getResKeyAsFallback.isNotEmpty()) strResKey else null
        }
    }

    /**
     * Get drawable from given ressource identifier
     */
    fun rdrawable(@DrawableRes resId: Int): Drawable? {
        try {
            return ContextCompat.getDrawable(_context!!, resId)
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Get color by given color ressource id
     */
    fun rcolor(@ColorRes resId: Int): Int {
        if (resId == 0) {
            Log.e(javaClass.name, "ContextUtils::rcolor: resId is 0!")
            return Color.BLACK
        }
        return ContextCompat.getColor(_context!!, resId)
    }

    /**
     * Checks if all given (textual) ressource ids are available
     *
     * @param resType       A [ResType]
     * @param resIdsTextual A (textual) identifier to be awaited at R.restype.resIdsTextual
     * @return True if all given ids are available
     */
    fun areRessourcesAvailable(resType: ResType, vararg resIdsTextual: String): Boolean {
        for (name in resIdsTextual) {
            if (getResId(resType, name) == 0) {
                return false
            }
        }
        return true
    }

    val appVersionName: String
        get() {
            val manager = _context!!.packageManager
            try {
                val info = manager.getPackageInfo(packageIdManifest, 0)
                return info.versionName
            } catch (e: PackageManager.NameNotFoundException) {
                try {
                    val info = manager.getPackageInfo(packageIdReal, 0)
                    return info.versionName
                } catch (ignored: PackageManager.NameNotFoundException) {
                }
            }
            return "?"
        }

    val appInstallationSource: String
        get() {
            var src: String? = null
            try {
                src = _context!!.packageManager.getInstallerPackageName(packageIdManifest)
            } catch (ignored: Exception) { }
            if (src == null || src.trim { it <= ' ' }.isEmpty()) {
                return "Sideloaded"
            } else if (src.toLowerCase().contains(".amazon.")) {
                return "Amazon Appstore"
            }
            when (src) {
                "com.android.vending", "com.google.android.feedback" -> {
                    return "Google Play"
                }
                "org.fdroid.fdroid.privileged", "org.fdroid.fdroid" -> {
                    return "F-Droid"
                }
                "com.github.yeriomin.yalpstore" -> {
                    return "Yalp Store"
                }
                "cm.aptoide.pt" -> {
                    return "Aptoide"
                }
                "com.android.packageinstaller" -> {
                    return "Package Installer"
                }
            }
            return src
        }

    /**
     * Send a [Intent.ACTION_VIEW] Intent with given paramter
     * If the parameter is an string a browser will get triggered
     */
    fun openWebpageInExternalBrowser(url: String): ContextUtils {
        try {
            val uri = Uri.parse(url)
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            _context!!.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return this
    }

    /**
     * Get the apps base packagename, which is equal with all build flavors and variants
     */
    val packageIdManifest: String
        get() {
            val pkg = rstr("manifest_package_id")
            return if (!TextUtils.isEmpty(pkg)) pkg!! else _context!!.packageName
        }

    /**
     * Get this apps package name, returns the flavor specific package name.
     */
    val packageIdReal: String
        get() = _context!!.packageName

    /**
     * Get field from ${applicationId}.BuildConfig
     * May be helpful in libraries, where a access to
     * BuildConfig would only get values of the library
     * rather than the app ones. It awaits a string resource
     * of the package set in manifest (root element).
     * Falls back to applicationId of the app which may differ from manifest.
     */
    fun getBuildConfigValue(fieldName: String): Any? {
        val pkg = "$packageIdManifest.BuildConfig"
        try {
            val c = Class.forName(pkg)
            return c.getField(fieldName).get(null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    val buildConfigFields: List<String>
        get() {
            val pkg = "$packageIdManifest.BuildConfig"
            val fields = ArrayList<String>()
            try {
                for (f in Class.forName(pkg).fields) {
                    fields.add(f.name)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return fields
        }

    /**
     * Get a BuildConfig bool value
     */
    fun bcbool(fieldName: String, defaultValue: Boolean?): Boolean? {
        val field = getBuildConfigValue(fieldName)
        if (field is Boolean) {
            return field
        }
        return defaultValue
    }

    /**
     * Get a BuildConfig string value
     */
    fun bcstr(fieldName: String, defaultValue: String): String {
        val field = getBuildConfigValue(fieldName)
        if (field is String) {
            return field
        }
        return defaultValue
    }

    /**
     * Get a BuildConfig string value
     */
    fun bcint(fieldName: String, defaultValue: Int): Int? {
        val field = getBuildConfigValue(fieldName)
        if (field is Int) {
            return field
        }
        return defaultValue
    }

    /**
     * Check if this is a gplay build (requires BuildConfig field)
     */
    fun isGooglePlayBuild(): Boolean {
        return bcbool("IS_GPLAY_BUILD", true)!!
    }

    /**
     * Check if this is a foss build (requires BuildConfig field)
     */
    fun isFossBuild(): Boolean {
        return bcbool("IS_FOSS_BUILD", false)!!
    }

    fun readTextfileFromRawRes(@RawRes rawResId: Int, linePrefix: String?, linePostfix: String?): String {
        var linePrefix = linePrefix
        var linePostfix = linePostfix
        val sb = StringBuilder()
        var br: BufferedReader? = null
        var line: String?

        linePrefix = linePrefix ?: ""
        linePostfix = linePostfix ?: ""

        try {
            br = BufferedReader(InputStreamReader(_context!!.resources.openRawResource(rawResId)))
            while (br.readLine().also { line = it } != null) {
                sb.append(linePrefix)
                sb.append(line)
                sb.append(linePostfix)
                sb.append("\n")
            }
        } catch (ignored: Exception) { }
        finally {
            if (br != null) {
                try {
                    br.close()
                } catch (ignored: IOException) { }
            }
        }
        return sb.toString()
    }

    /**
     * Get internet connection state - the permission ACCESS_NETWORK_STATE is required
     *
     * @return True if internet connection available
     */
    val isConnectedToInternet: Boolean
        get() {
            try {
                val con = _context!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                @SuppressLint("MissingPermission") val activeNetInfo = con.activeNetworkInfo
                return activeNetInfo != null && activeNetInfo.isConnectedOrConnecting
            } catch (ignored: Exception) {
                throw RuntimeException("Error: Developer forgot to declare a permission")
            }
        }

    /**
     * Check if app with given `packageName` is installed
     */
    fun isAppInstalled(packageName: String): Boolean {
        return try {
            val pm = _context!!.applicationContext.packageManager
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Restart the current app. Supply the class to start on startup
     */
    fun restartApp(classToStart: Class<*>) {
        val intent = Intent(_context, classToStart)
        val pendi = PendingIntent.getActivity(_context, 555, intent, PendingIntent.FLAG_CANCEL_CURRENT)
        val mgr = _context!!.getSystemService(Context.ALARM_SERVICE) as AlarmManager?
        if (_context is Activity) {
            (_context as Activity).finish()
        }
        if (mgr != null) {
            mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, pendi)
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            _context!!.startActivity(intent)
        }
        Runtime.getRuntime().exit(0)
    }

    /**
     * Load a markdown file from a [RawRes], prepend each line with `prepend` text
     * and convert markdown to html using [SimpleMarkdownParser]
     */
    fun loadMarkdownForTextViewFromRaw(@RawRes rawMdFile: Int, prepend: String): String {
        try {
            return SimpleMarkdownParser()
                .parse(
                    _context!!.resources.openRawResource(rawMdFile),
                    prepend, SimpleMarkdownParser.FILTER_ANDROID_TEXTVIEW
                )
                .replaceColor("#000001", rcolor(getResId(ResType.COLOR, "accent")))
                .removeMultiNewlines().replaceBulletCharacter("*").html
        } catch (e: IOException) {
            e.printStackTrace()
            return ""
        }
    }

    /**
     * Load html into a [Spanned] object and set the
     * [TextView]'s text using [TextView.setText]
     */
    fun setHtmlToTextView(textView: TextView, html: String) {
        textView.movementMethod = LinkMovementMethod.getInstance()
        textView.text = SpannableString(htmlToSpanned(html))
    }

    /**
     * Estimate this device's screen diagonal size in inches
     */
    val estimatedScreenSizeInches: Double
        get() {
            val dm = _context!!.resources.displayMetrics

            var calc = (dm.density * 160f).toDouble()
            val x = Math.pow((dm.widthPixels / calc), 2.0)
            val y = Math.pow((dm.heightPixels / calc), 2.0)
            calc = Math.sqrt(x + y) * 1.16  // 1.16 = est. Nav/Statusbar
            return Math.min(12.0, Math.max(4.0, calc))
        }

    /**
     * Check if the device is currently in portrait orientation
     */
    val isInPortraitMode: Boolean
        get() = _context!!.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT

    /**
     * Get an [Locale] out of a android language code
     * The `androidLC` may be in any of the forms: de, en, de-rAt
     */
    fun getLocaleByAndroidCode(androidLC: String?): Locale {
        if (!TextUtils.isEmpty(androidLC)) {
            return if (androidLC!!.contains("-r")) Locale(
                androidLC.substring(0, 2),
                androidLC.substring(4, 6)
            ) else Locale(androidLC) // de
        }
        return Resources.getSystem().configuration.locale
    }

    /**
     * Set the apps language
     * `androidLC` may be in any of the forms: en, de, de-rAt
     * If given an empty string, the default (system) locale gets loaded
     */
    fun setAppLanguage(androidLC: String) {
        var locale: Locale? = getLocaleByAndroidCode(androidLC)
        locale = if (locale != null && androidLC.isNotEmpty()) locale else Resources.getSystem().configuration.locale
        setLocale(locale)
    }

    fun setLocale(locale: Locale?): ContextUtils {
        var locale = locale
        val config = _context!!.resources.configuration
        config.locale = locale ?: Resources.getSystem().configuration.locale
        _context!!.resources.updateConfiguration(config, null)
        Locale.setDefault(locale)
        return this
    }

    /**
     * Try to guess if the color on top of the given `colorOnBottomInt`
     * should be light or dark. Returns true if top color should be light
     */
    fun shouldColorOnTopBeLight(@ColorInt colorOnBottomInt: Int): Boolean {
        return 186 > (0.299 * Color.red(colorOnBottomInt)
                + (0.587 * Color.green(colorOnBottomInt)
                + 0.114 * Color.blue(colorOnBottomInt)))
    }

    /**
     * Convert a html string to an android [Spanned] object
     */
    fun htmlToSpanned(html: String): Spanned {
        val result: Spanned
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            result = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        } else {
            result = Html.fromHtml(html)
        }
        return result
    }

    /**
     * Convert pixel unit do android dp unit
     */
    fun convertPxToDp(px: Float): Float {
        return px / _context!!.resources.displayMetrics.density
    }

    /**
     * Convert android dp unit to pixel unit
     */
    fun convertDpToPx(dp: Float): Float {
        return dp * _context!!.resources.displayMetrics.density
    }

    /**
     * Get the private directory for the current package (usually /data/data/package.name/)
     */
    val appDataPrivateDir: File
        get() {
            var filesDir: File
            try {
                filesDir = File(
                    File(_context!!.packageManager.getPackageInfo(packageIdReal, 0).applicationInfo.dataDir),
                    "files"
                )
            } catch (e: PackageManager.NameNotFoundException) {
                filesDir = _context!!.filesDir
            }
            if (!filesDir.exists() && filesDir.mkdirs());
            return filesDir
        }

    /**
     * Get public (accessible) appdata folders
     */
    fun getAppDataPublicDirs(
        internalStorageFolder: Boolean,
        sdcardFolders: Boolean,
        storageNameWithoutType: Boolean
    ): List<Pair<File, String>> {
        val dirs = ArrayList<Pair<File, String>>()
        for (externalFileDir in ContextCompat.getExternalFilesDirs(_context!!, null)) {
            if (externalFileDir == null || Environment.getExternalStorageDirectory() == null) {
                continue
            }
            val isInt = externalFileDir.absolutePath.startsWith(Environment.getExternalStorageDirectory().absolutePath)
            val add = internalStorageFolder && isInt || sdcardFolders && !isInt
            if (add) {
                dirs.add(Pair(externalFileDir, getStorageName(externalFileDir, storageNameWithoutType)))
                if (!externalFileDir.exists() && externalFileDir.mkdirs());
            }
        }
        return dirs
    }

    fun getStorageName(externalFileDir: File, storageNameWithoutType: Boolean): String {
        val isInt = externalFileDir.absolutePath.startsWith(Environment.getExternalStorageDirectory().absolutePath)

        val split = externalFileDir.absolutePath.split("/"[0]).dropLastWhile { it.isEmpty() }.toTypedArray()
        return if (split.size > 2) {
            if (isInt) (if (storageNameWithoutType) "Internal Storage" else "") else if (storageNameWithoutType) split[2] else "SD Card (" + split[2] + ")"
        } else {
            "Storage"
        }
    }

    fun getStorages(internalStorageFolder: Boolean, sdcardFolders: Boolean): List<Pair<File, String>> {
        val storages = ArrayList<Pair<File, String>>()
        for (pair in getAppDataPublicDirs(internalStorageFolder, sdcardFolders, true)) {
            if (pair.first != null && pair.first!!.absolutePath.lastIndexOf("/Android/data") > 0) {
                try {
                    storages.add(
                        Pair(
                            File(pair.first!!.canonicalPath.replaceFirst("/Android/data.*".toRegex(), "")),
                            pair.second
                        )
                    )
                } catch (ignored: IOException) { }
            }
        }
        return storages
    }

    fun getStorageRootFolder(file: File): File? {
        val filepath: String
        try {
            filepath = file.canonicalPath
        } catch (ignored: Exception) {
            return null
        }
        for (storage in getStorages(false, true)) {
            //noinspection ConstantConditions
            if (filepath.startsWith(storage.first.absolutePath)) {
                return storage.first
            }
        }
        return null
    }

    /**
     * Request the givens paths to be scanned by MediaScanner
     *
     * @param files Files and folders to scan
     */
    fun mediaScannerScanFile(vararg files: File) {
        if (Build.VERSION.SDK_INT > 19) {
            val paths = Array(files.size) { i -> files[i].absolutePath }
            MediaScannerConnection.scanFile(_context, paths, null, null)
        } else {
            for (file in files) {
                _context!!.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)))
            }
        }
    }

    /**
     * Get a [Bitmap] out of a [Drawable]
     */
    fun drawableToBitmap(drawable: Drawable): Bitmap? {
        var bitmap: Bitmap? = null
        if (drawable is VectorDrawableCompat
            || Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && drawable is VectorDrawable
            || Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable
        ) {

            var drawable = drawable
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                drawable = DrawableCompat.wrap(drawable).mutate()
            }

            bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight, Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
        } else if (drawable is BitmapDrawable) {
            bitmap = drawable.bitmap
        }
        return bitmap
    }

    /**
     * Get a [Bitmap] out of a [DrawableRes]
     */
    fun drawableToBitmap(@DrawableRes drawableId: Int): Bitmap? {
        try {
            return drawableToBitmap(ContextCompat.getDrawable(_context!!, drawableId)!!)
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Get a [Bitmap] from a given `imagePath` on the filesystem
     * Specifying a `maxDimen` is also possible and a value below 2000
     * is recommended, otherwise a [OutOfMemoryError] may occur
     */
    fun loadImageFromFilesystem(imagePath: File, maxDimen: Int): Bitmap {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(imagePath.absolutePath, options)
        options.inSampleSize = calculateInSampleSize(options, maxDimen)
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(imagePath.absolutePath, options)
    }

    /**
     * Calculates the scaling factor so the bitmap is maximal as big as the maxDimen
     *
     * @param options  Bitmap-options that contain the current dimensions of the bitmap
     * @param maxDimen Max size of the Bitmap (width or height)
     * @return the scaling factor that needs to be applied to the bitmap
     */
    fun calculateInSampleSize(options: BitmapFactory.Options, maxDimen: Int): Int {
        // Raw height and width of image
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (Math.max(height, width) > maxDimen) {
            inSampleSize = Math.round(1f * Math.max(height, width) / maxDimen)
        }
        return inSampleSize
    }

    /**
     * Scale the bitmap so both dimensions are lower or equal to `maxDimen`
     * This keeps the aspect ratio
     */
    fun scaleBitmap(bitmap: Bitmap, maxDimen: Int): Bitmap {
        val picSize = Math.min(bitmap.height, bitmap.width)
        val scale = 1f * maxDimen / picSize
        val matrix = Matrix()
        matrix.postScale(scale, scale)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Write the given [Bitmap] to filesystem
     *
     * @param targetFile The file to be written in
     * @param image      Android [Bitmap]
     * @return True if writing was successful
     */
    fun writeImageToFile(targetFile: File, image: Bitmap, vararg a0quality: Int?): Boolean {
        val quality =
            if (a0quality.isNotEmpty() && a0quality[0]!! >= 0 && a0quality[0]!! <= 100) a0quality[0] else 70
        val lc = targetFile.absolutePath.toLowerCase(Locale.ROOT)
        val format =
            if (lc.endsWith(".webp")) Bitmap.CompressFormat.WEBP else if (lc.endsWith(".png")) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG

        var ok = false
        val folder = File(targetFile.parent)
        if (folder.exists() || folder.mkdirs()) {
            var stream: FileOutputStream? = null
            try {
                stream = FileOutputStream(targetFile)
                image.compress(format, quality!!, stream)
                ok = true
            } catch (ignored: Exception) { }
            finally {
                try {
                    stream?.close()
                } catch (ignored: IOException) { }
            }
        }
        try {
            image.recycle()
        } catch (ignored: Exception) { }
        return ok
    }

    /**
     * Draw text in the center of the given [DrawableRes]
     * This may be useful for e.g. badge counts
     */
    fun drawTextOnDrawable(@DrawableRes drawableRes: Int, text: String, textSize: Int): Bitmap {
        val resources = _context!!.resources
        val scale = resources.displayMetrics.density
        var bitmap = drawableToBitmap(drawableRes)

        bitmap = bitmap!!.copy(bitmap.config, true)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.rgb(61, 61, 61)
        paint.textSize = (textSize * scale)
        paint.setShadowLayer(1f, 0f, 1f, Color.WHITE)

        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)
        val x = (bitmap.width - bounds.width()) / 2
        val y = (bitmap.height + bounds.height()) / 2
        canvas.drawText(text, x.toFloat(), y.toFloat(), paint)

        return bitmap
    }

    /**
     * Try to tint all [Menu]s [MenuItem]s with given color
     */
    fun tintMenuItems(menu: Menu, recurse: Boolean, @ColorInt iconColor: Int) {
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            try {
                tintDrawable(item.icon, iconColor)
                if (item.hasSubMenu() && recurse) {
                    tintMenuItems(item.subMenu, recurse, iconColor)
                }
            } catch (ignored: Exception) {
                // This should not happen at all, but may in bad menu.xml configuration
            }
        }
    }

    /**
     * Loads [Drawable] by given [DrawableRes] and applies a color
     */
    fun tintDrawable(@DrawableRes drawableRes: Int, @ColorInt color: Int): Drawable? {
        return tintDrawable(rdrawable(drawableRes), color)
    }

    /**
     * Tint a [Drawable] with given `color`
     */
    fun tintDrawable(drawable: Drawable?, @ColorInt color: Int): Drawable? {
        var drawable = drawable
        if (drawable != null) {
            drawable = DrawableCompat.wrap(drawable)
            DrawableCompat.setTint(drawable.mutate(), color)
        }
        return drawable
    }

    /**
     * Try to make icons in Toolbar/ActionBars SubMenus visible
     * This may not work on some devices and it maybe won't work on future android updates
     */
    fun setSubMenuIconsVisiblity(menu: Menu, visible: Boolean) {
        if (TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault()) == ViewCompat.LAYOUT_DIRECTION_RTL) {
            return
        }
        if (menu.javaClass.simpleName == "MenuBuilder") {
            try {
                @SuppressLint("PrivateApi") val m = 
                    menu.javaClass.getDeclaredMethod("setOptionalIconsVisible", Boolean::class.javaPrimitiveType)
                m.isAccessible = true
                m.invoke(menu, visible)
            } catch (ignored: Exception) {
                Log.d(javaClass.name, "Error: 'setSubMenuIconsVisiblity' not supported on this device")
            }
        }
    }


    val localizedDateFormat: String
        get() = (android.text.format.DateFormat.getDateFormat(_context) as SimpleDateFormat).toPattern()

    val localizedTimeFormat: String
        get() = (android.text.format.DateFormat.getTimeFormat(_context) as SimpleDateFormat).toPattern()

    val localizedDateTimeFormat: String
        get() = "$localizedDateFormat $localizedTimeFormat"

    /**
     * A simple [Runnable] which does a touch event on a view.
     * This pops up e.g. the keyboard on a [android.widget.EditText]
     *
     *
     * Example: new Handler().postDelayed(new DoTouchView(editView), 200);
     */
    class DoTouchView(internal var _view: View) : Runnable {

        override fun run() {
            _view.dispatchTouchEvent(
                MotionEvent.obtain(
                    SystemClock.uptimeMillis(),
                    SystemClock.uptimeMillis(),
                    MotionEvent.ACTION_DOWN,
                    0f,
                    0f,
                    0
                )
            )
            _view.dispatchTouchEvent(
                MotionEvent.obtain(
                    SystemClock.uptimeMillis(),
                    SystemClock.uptimeMillis(),
                    MotionEvent.ACTION_UP,
                    0f,
                    0f,
                    0
                )
            )
        }
    }


    fun getMimeType(file: File): String {
        return getMimeType(Uri.fromFile(file))
    }

    /**
     * Detect MimeType of given file
     * Android/Java's own MimeType map is very very small and detection barely works at all
     * Hence use custom map for some file extensions
     */
    fun getMimeType(uri: Uri): String {
        var mimeType: String? = null
        if (ContentResolver.SCHEME_CONTENT == uri.scheme) {
            val cr = _context!!.contentResolver
            mimeType = cr.getType(uri)
        } else {
            var filename = uri.toString()
            if (filename.endsWith(".jenc")) {
                filename = filename.replace(".jenc", "")
            }
            val ext = MimeTypeMap.getFileExtensionFromUrl(filename)
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase())

            // Try to guess if the recommended methods fail
            if (TextUtils.isEmpty(mimeType)) {
                when (ext) {
                    "md", "markdown", "mkd", "mdown", "mkdn", "mdwn", "rmd" -> mimeType = "text/markdown"
                    "yaml", "yml" -> mimeType = "text/yaml"
                    "json" -> mimeType = "text/json"
                    "txt" -> mimeType = "text/plain"
                }
            }
        }

        if (TextUtils.isEmpty(mimeType)) {
            mimeType = "*/*"
        }
        return mimeType!!
    }

    fun parseColor(colorstr: String?): Int? {
        if (colorstr == null || colorstr.trim { it <= ' ' }.isEmpty()) {
            return null
        }
        try {
            return Color.parseColor(colorstr)
        } catch (ignored: IllegalArgumentException) {
            return null
        }
    }

    val isDeviceGoodHardware: Boolean
        get() {
            try {
                val activityManager = _context!!.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                return !ActivityManagerCompat.isLowRamDevice(activityManager) &&
                        Runtime.getRuntime().availableProcessors() >= 4 &&
                        activityManager.memoryClass >= 128
            } catch (ignored: Exception) {
                return true
            }
        }

    // Vibrate device one time by given amount of time, defaulting to 50ms
    // Requires <uses-permission android:name="android.permission.VIBRATE" /> in AndroidManifest to work
    @SuppressLint("MissingPermission")
    fun vibrate(vararg ms: Int) {
        val ms_v = if (ms.isNotEmpty()) ms[0] else 50
        val vibrator = _context!!.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
        if (vibrator == null) {
            return
        } else if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(ms_v.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(ms_v.toLong())
        }
    }

    /*
    Check if Wifi is connected. Requires these permissions in AndroidManifest:
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
     */
    @SuppressLint("MissingPermission")
    fun isWifiConnected(vararg enabledOnly: Boolean): Boolean {
        val doEnabledCheckOnly = enabledOnly.isNotEmpty() && enabledOnly[0]
        val connectivityManager = 
            _context!!.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wifiInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        return wifiInfo != null && if (doEnabledCheckOnly) wifiInfo.isAvailable else wifiInfo.isConnected
    }

    // Returns if the device is currently in portrait orientation (landscape=false)
    val isDeviceOrientationPortrait: Boolean
        get() {
            val rotation = (_context!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.orientation
            return rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180
        }

    companion object {

        /**
         * Convert an int color to a hex string. Optionally including alpha value.
         *
         * @param intColor  The color coded in int
         * @param withAlpha Optional; Set first bool parameter to true to also include alpha value
         */
        fun colorToHexString(intColor: Int, vararg withAlpha: Boolean): String {
            val a = withAlpha.isNotEmpty() && withAlpha[0]
            return String.format(if (a) "#%08X" else "#%06X", (if (a) -0x1 else 0xFFFFFF) and intColor)
        }

        val androidVersion: String
            get() = Build.VERSION.RELEASE + " (" + Build.VERSION.SDK_INT + ")"

        /**
         * Load an image into a [ImageView] and apply a color filter
         */
        fun setDrawableWithColorToImageView(
            imageView: ImageView,
            @DrawableRes drawableResId: Int,
            @ColorRes colorResId: Int
        ) {
            imageView.setImageResource(drawableResId)
            imageView.setColorFilter(ContextCompat.getColor(imageView.context, colorResId))
        }

        /**
         * A [InputFilter] for filenames
         */
        val INPUTFILTER_FILENAME: InputFilter = object : InputFilter {
            override fun filter(
                src: CharSequence,
                start: Int,
                end: Int,
                dest: Spanned,
                dstart: Int,
                dend: Int
            ): CharSequence? {
                if (src.isEmpty()) return null
                val last = src[src.length - 1]
                val illegal = "|\?*<\":>[]/'"
                return if (illegal.indexOf(last) > -1) src.subSequence(0, src.length - 1) else null
            }
        }
    }
}