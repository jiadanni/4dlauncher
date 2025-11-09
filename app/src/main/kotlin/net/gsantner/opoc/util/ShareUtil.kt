/*#######################################################
 * 
 *   Maintained 2017-2023 by Gregor Santner <gsantner @ mailbox . org>
 * 
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *     https://github.com/gsantner/opoc/#licensing
 * 
#########################################################*/
package net.gsantner.opoc.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintJob
import android.print.PrintManager
import android.provider.CalendarContract
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.util.Pair
import androidx.documentfile.provider.DocumentFile
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.preference.PreferenceManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random

/**
 * A utility class to ease information sharing on Android.
 * Also allows to parse/fetch information out of shared information.
 * (M)Permissions are not checked, wrap ShareUtils methods if neccessary
 */
@Suppress(
    "UnusedReturnValue",
    "WeakerAccess",
    "SameParameterValue",
    "unused",
    "deprecation",
    "ConstantConditions",
    "ObsoleteSdkInt",
    "SpellCheckingInspection",
    "JavadocReference",
    "ConstantLocale"
)
class ShareUtil(protected var _context: Context?) {
    protected var _chooserTitle: String = "➥"

    fun freeContextRef() {
        _context = null
    }

    val fileProviderAuthority: String
        get() {
            if (TextUtils.isEmpty(_fileProviderAuthority)) {
                throw RuntimeException("Error at ShareUtil.getFileProviderAuthority(): No FileProvider authority provided")
            }
            return _fileProviderAuthority!!
        }

    fun setChooserTitle(title: String): ShareUtil {
        _chooserTitle = title
        return this
    }

    /**
     * Convert a [File] to an [Uri]
     *
     * @param file the file
     * @return Uri for this file
     */
    fun getUriByFileProviderAuthority(file: File): Uri {
        return FileProvider.getUriForFile(_context!!, fileProviderAuthority, file)
    }

    /**
     * Allow to choose a handling app for given intent
     *
     * @param intent      Thing to be shared
     * @param chooserText The title text for the chooser, or null for default
     */
    fun showChooser(intent: Intent, chooserText: String?) {
        try {
            _context!!.startActivity(Intent.createChooser(intent, chooserText ?: _chooserTitle))
        } catch (ignored: Exception) { 
        }
    }

    /**
     * Try to create a new desktop shortcut on the launcher. Add permissions:
     * <uses-permission android:name="android.permission.INSTALL_SHORTCUT"></uses-permission>
     * <uses-permission android:name="com.android.launcher.permission.INSTALL_SHORTCUT"></uses-permission>
     *
     * @param intent  The intent to be invoked on tap
     * @param iconRes Icon resource for the item
     * @param title   Title of the item
     */
    fun createLauncherDesktopShortcut(intent: Intent, @DrawableRes iconRes: Int, title: String) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (intent.action == null) {
            intent.action = Intent.ACTION_VIEW
        }

        val shortcut = ShortcutInfoCompat.Builder(_context!!, Random().nextLong().toString())
            .setIntent(intent)
            .setIcon(IconCompat.createWithResource(_context!!, iconRes))
            .setShortLabel(title)
            .setLongLabel(title)
            .build()
        ShortcutManagerCompat.requestPinShortcut(_context!!, shortcut, null)
    }

    /**
     * Try to create a new desktop shortcut on the launcher. This will not work on Api > 25. Add permissions:
     * <uses-permission android:name="android.permission.INSTALL_SHORTCUT"></uses-permission>
     * <uses-permission android:name="com.android.launcher.permission.INSTALL_SHORTCUT"></uses-permission>
     *
     * @param intent  The intent to be invoked on tap
     * @param iconRes Icon resource for the item
     * @param title   Title of the item
     */
    fun createLauncherDesktopShortcutLegacy(intent: Intent, @DrawableRes iconRes: Int, title: String) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (intent.action == null) {
            intent.action = Intent.ACTION_VIEW
        }

        val creationIntent = Intent("com.android.launcher.action.INSTALL_SHORTCUT")
        creationIntent.putExtra("duplicate", true)
        creationIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent)
        creationIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, title)
        creationIntent.putExtra(
            Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
            Intent.ShortcutIconResource.fromContext(_context!!, iconRes)
        )
        _context!!.sendBroadcast(creationIntent)
    }

    /**
     * Share text with given mime-type
     *
     * @param text     The text to share
     * @param mimeType MimeType or null (uses text/plain)
     */
    fun shareText(text: String, mimeType: String?) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_TEXT, text)
        intent.type = mimeType ?: MIME_TEXT_PLAIN
        showChooser(intent, null)
    }

    /**
     * Share the given file as stream with given mime-type
     *
     * @param file     The file to share
     * @param mimeType The files mime type
     */
    fun shareStream(file: File, mimeType: String): Boolean {
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(EXTRA_FILEPATH, file.absolutePath)
        intent.type = mimeType

        try {
            val fileUri = FileProvider.getUriForFile(_context!!, fileProviderAuthority, file)
            intent.putExtra(Intent.EXTRA_STREAM, fileUri)
            showChooser(intent, null)
            return true
        } catch (ignored: Exception) { // FileUriExposed(API24) / IllegalArgument
        }
        return false
    }

    /**
     * Share the given files as stream with given mime-type
     *
     * @param files    The files to share
     * @param mimeType The files mime type. Usally * / * is the best option
     */
    fun shareStreamMultiple(files: Collection<File>, mimeType: String): Boolean {
        val uris = ArrayList<Uri>()
        for (file in files) {
            uris.add(FileProvider.getUriForFile(_context!!, fileProviderAuthority, file))
        }

        try {
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE)
            intent.type = mimeType
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            showChooser(intent, null)
            return true
        } catch (e: Exception) { // FileUriExposed(API24) / IllegalArgument
            return false
        }
    }

    /**
     * Start calendar application to add new event, with given details prefilled
     */
    fun createCalendarAppointment(
        title: String?,
        description: String?,
        location: String?,
        vararg startAndEndTime: Long?
    ): Boolean {
        val intent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
        if (title != null) {
            intent.putExtra(CalendarContract.Events.TITLE, title)
        }
        if (description != null) {
            intent.putExtra(
                CalendarContract.Events.DESCRIPTION,
                if (description.length > 800) description.substring(0, 800) else description
            )
        }
        if (location != null) {
            intent.putExtra(CalendarContract.Events.EVENT_LOCATION, location)
        }
        if (startAndEndTime.isNotEmpty() && startAndEndTime[0]!! > 0) {
            intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startAndEndTime[0])
        }
        if (startAndEndTime.size > 1 && startAndEndTime[1]!! > 0) {
            intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, startAndEndTime[1])
        }

        try {
            _context!!.startActivity(intent)
            return true
        } catch (e: ActivityNotFoundException) {
            return false
        }
    }

    /**
     * Open a View intent for given file
     *
     * @param file The file to share
     */
    fun viewFileInOtherApp(file: File, type: String?): Boolean {
        // On some specific devices the first won't work
        var fileUri: Uri?
        try {
            fileUri = FileProvider.getUriForFile(_context!!, fileProviderAuthority, file)
        } catch (ignored: Exception) { 
            try {
                fileUri = Uri.fromFile(file)
            } catch (ignored2: Exception) {
                fileUri = null
            }
        }

        if (fileUri != null) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.putExtra(Intent.EXTRA_STREAM, fileUri)
            intent.data = fileUri
            intent.putExtra(EXTRA_FILEPATH, file.absolutePath)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.setDataAndType(fileUri, type)
            showChooser(intent, null)
            return true
        }
        return false
    }

    /**
     * Share the given bitmap with given format
     *
     * @param bitmap    Image
     * @param format    A [Bitmap.CompressFormat], supporting JPEG,PNG,WEBP
     * @param imageName Filename without extension
     * @param quality   Quality of the exported image [0-100]
     * @return if success, true
     */
    fun shareImage(bitmap: Bitmap?, vararg quality: Int?): Boolean {
        try {
            val file = File(_context!!.cacheDir, getFilenameWithTimestamp())
            if (bitmap != null && ContextUtils(_context).writeImageToFile(file, bitmap, *quality)) {
                shareStream(file, FileUtils.getMimeType(file))
                return true
            }
        } catch (ignored: Exception) { 
        }
        return false
    }

    /**
     * Print a [WebView]'s contents, also allows to create a PDF
     *
     * @param webview WebView
     * @param jobName Name of the job (affects PDF name too)
     * @return [[PrintJob]] or null
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    fun print(webview: WebView, jobName: String, vararg landscape: Boolean): PrintJob? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val printAdapter: PrintDocumentAdapter
            val printManager = _context!!.getSystemService(Context.PRINT_SERVICE) as PrintManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                printAdapter = webview.createPrintDocumentAdapter(jobName)
            } else {
                printAdapter = webview.createPrintDocumentAdapter()
            }
            val attrib = PrintAttributes.Builder()
            if (landscape.isNotEmpty() && landscape[0]) {
                attrib.setMediaSize(PrintAttributes.MediaSize("ISO_A4", "android", 11690, 8270))
                attrib.setMinMargins(PrintAttributes.Margins(0, 0, 0, 0))
            }
            try {
                return printManager.print(jobName, printAdapter, attrib.build())
            } catch (ignored: Exception) { 
            }
        } else { 
            Log.e(javaClass.name, "ERROR: Method called on too low Android API version")
        }
        return null
    }


    /**
     * See [print]
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    fun createPdf(webview: WebView, jobName: String): PrintJob? {
        return print(webview, jobName)
    }


    /*** 
     * Replace (primary) clipboard contents with given `text`
     * @param text Text to be set
     */
    fun setClipboard(text: CharSequence): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            val cm = _context!!.getSystemService(Context.CLIPBOARD_SERVICE) as android.text.ClipboardManager?
            if (cm != null) {
                cm.text = text
                return true
            }
        } else {
            val cm = _context!!.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager?
            if (cm != null) {
                val clip = ClipData.newPlainText(_context!!.packageName, text)
                try {
                    cm.setPrimaryClip(clip)
                } catch (ignored: Exception) { 
                }
                return true
            }
        }
        return false
    }

    /**
     * Get clipboard contents, very failsafe and compat to older android versions
     */
    val clipboard: List<String>
        get() {
            val clipper = ArrayList<String>()
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                val cm = _context!!.getSystemService(Context.CLIPBOARD_SERVICE) as android.text.ClipboardManager?
                if (cm != null && !TextUtils.isEmpty(cm.text)) {
                    clipper.add(cm.text.toString())
                }
            } else {
                val cm = _context!!.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager?
                if (cm != null && cm.hasPrimaryClip()) {
                    val data = cm.primaryClip
                    var i = 0
                    while (data != null && i < data.itemCount && i < data.itemCount) {
                        val item = data.getItemAt(i)
                        if (item != null && !TextUtils.isEmpty(item.text)) {
                            clipper.add(data.getItemAt(i).text.toString())
                        }
                        i++
                    }
                }
            }
            return clipper
        }

    /**
     * Share given text on a hastebin compatible server
     * (https://github.com/seejohnrun/haste-server)
     * Permission needed: Internet
     * Pastes will be deleted after 30 days without access
     *
     * @param text            The text to paste
     * @param callback        Callback after paste try
     * @param serverOrNothing Supply one or no hastebin server. If empty, the default gets taken
     */
    fun pasteOnHastebin(text: String, callback: Callback.a2<Boolean, String>, vararg serverOrNothing: String?) {
        val handler = Handler()
        val server = 
            if (serverOrNothing.isNotEmpty() && serverOrNothing[0] != null) serverOrNothing[0] else "https://hastebin.com"
        Thread {
            // Returns a simple result, handleable without json parser {"key":"feediyujiq"}
            val ret = NetworkUtils.performCall("$server/documents", NetworkUtils.POST, text)
            val key = if (ret.length > 15) ret.split("\"".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()[3] else ""
            handler.post { callback.callback(key.isNotEmpty(), "$server/$key") }
        }.start()
    }

    /**
     * Draft an email with given data. Unknown data can be supplied as null.
     * This will open a chooser with installed mail clients where the mail can be sent from
     *
     * @param subject Subject (top/title) text to be prefilled in the mail
     * @param body    Body (content) text to be prefilled in the mail
     * @param to      recipients to be prefilled in the mail
     */
    fun draftEmail(subject: String?, body: String?, vararg to: String?) {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:")
        if (subject != null) {
            intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        }
        if (body != null) {
            intent.putExtra(Intent.EXTRA_TEXT, body)
        }
        if (to.isNotEmpty() && to[0] != null) {
            intent.putExtra(Intent.EXTRA_EMAIL, to)
        }
        showChooser(intent, null)
    }

    /**
     * Try to force extract a absolute filepath from an intent
     *
     * @param receivingIntent The intent from [Activity.getIntent]
     * @return A file or null if extraction did not succeed
     */
    fun extractFileFromIntent(receivingIntent: Intent): File? {
        val action = receivingIntent.action
        var tmps: String
        var fileStr: String?

        if (Intent.ACTION_VIEW == action || Intent.ACTION_EDIT == action || Intent.ACTION_SEND == action) {
            // Markor, S.M.T FileManager
            if (receivingIntent.hasExtra(EXTRA_FILEPATH.also { tmps = it })) {
                return File(receivingIntent.getStringExtra(tmps)!!)
            }

            // Analyze data/Uri
            var fileUri = receivingIntent.data
            if (fileUri != null && fileUri.toString().also { fileStr = it } != null) {
                // Uri contains file
                if (fileStr!!.startsWith("file://")) {
                    return File(fileUri.path!!)
                }
                if (fileStr!!.startsWith("content://".also { tmps = it })) {
                    fileStr = fileStr!!.substring(tmps.length)
                    val fileProvider = fileStr!!.substring(0, fileStr!!.indexOf("/"))
                    fileStr = fileStr!!.substring(fileProvider.length + 1)

                    // Some file managers dont add leading slash
                    if (fileStr!!.startsWith("storage/")) {
                        fileStr = "/$fileStr"
                    }
                    // Some do add some custom prefix
                    for (prefix in arrayOf("file", "document", "root_files", "name")) {
                        if (fileStr!!.startsWith(prefix)) {
                            fileStr = fileStr!!.substring(prefix.length)
                        }
                    }

                    // prefix for External storage (/storage/emulated/0  ///  /sdcard/) --> e.g. "content://com.amaze.filemanager/storage_root/file.txt" = "/sdcard/file.txt"
                    for (prefix in arrayOf("external/", "media/", "storage_root/")) {
                        if (fileStr!!.startsWith(prefix.also { tmps = it })) {
                            val f = File(
                                Uri.decode(
                                    Environment.getExternalStorageDirectory().absolutePath + "/" + fileStr!!.substring(
                                        tmps.length
                                    )
                                )
                            )
                            if (f.exists()) {
                                return f
                            }
                        }
                    }

                    // Next/OwnCloud Fileprovider
                    for (fp in arrayOf(
                        "org.nextcloud.files",
                        "org.nextcloud.beta.files",
                        "org.owncloud.files"
                    )) {
                        if (fileProvider == fp && fileStr!!.startsWith("external_files/".also { tmps = it })) {
                            return File(Uri.decode("/storage/" + fileStr!!.substring(
                                tmps.length
                            )))
                        }
                    }
                    // AOSP File Manager/Documents
                    if (fileProvider == "com.android.externalstorage.documents" && fileStr!!.startsWith("/primary%3A".also { tmps = it })) {
                        return File(
                            Uri.decode(
                                Environment.getExternalStorageDirectory().absolutePath + "/" + fileStr!!.substring(
                                    tmps.length
                                )
                            )
                        )
                    }
                    // Mi File Explorer
                    if (fileProvider == "com.mi.android.globalFileexplorer.myprovider" && fileStr!!.startsWith("external_files".also { tmps = it })) {
                        return File(
                            Uri.decode(
                                Environment.getExternalStorageDirectory().absolutePath + fileStr!!.substring(
                                    tmps.length
                                )
                            )
                        )
                    }

                    if (fileStr!!.startsWith("external_files/".also { tmps = it })) {
                        for (prefix in arrayOf(
                            Environment.getExternalStorageDirectory().absolutePath,
                            "/storage",
                            ""
                        )) {
                            val f = File(Uri.decode(prefix + "/" + fileStr!!.substring(tmps.length)))
                            if (f.exists()) {
                                return f
                            }
                        }

                    }

                    // URI Encoded paths with full path after content://package/
                    if (fileStr!!.startsWith("/" ) || fileStr!!.startsWith("%2F")) {
                        var tmpf: File? = null
                        var f: File? = null
                        try {
                            f = File(Uri.decode(fileStr))
                        } catch (e: Exception) { /* ignore */ }
                        if (f != null && f.exists()) {
                            return f
                        } else if (File(fileStr!!).also { tmpf = it }.exists()) {
                            return tmpf
                        }
                    }
                }
            }
            fileUri = receivingIntent.getParcelableExtra(Intent.EXTRA_STREAM)
            var tmpf: File? = null
            var f: File? = null
            try {
                f = File(Uri.decode(receivingIntent.getStringExtra(EXTRA_FILEPATH)))
            } catch (e: Exception) { /* ignore */ }
            if (f != null && f.exists()) {
                return f
            } else if (File(receivingIntent.getStringExtra(EXTRA_FILEPATH)!!).also { tmpf = it }.exists()) {
                return tmpf
            }
        }
        return null
    }

    /**
     * Request a picture from gallery
     * Result will be available from [Activity.onActivityResult].
     * It will return the path to the image if locally stored. If retrieved from e.g. a cloud
     * service, the image will get copied to app-cache folder and it's path returned.
     */
    fun requestGalleryPicture() {
        if (_context !is Activity) {
            throw RuntimeException("Error: ShareUtil.requestGalleryPicture needs an Activity Context.")
        }
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        try {
            (_context as Activity).startActivityForResult(intent, REQUEST_PICK_PICTURE)
        } catch (ex: Exception) {
            Toast.makeText(_context, "No gallery app installed!", Toast.LENGTH_SHORT).show()
        }
    }

    fun extractFileFromIntentStr(receivingIntent: Intent): String? {
        val f = extractFileFromIntent(receivingIntent)
        return f?.absolutePath
    }

    /**
     * Request a picture from camera-like apps
     * Result ([String]) will be available from [Activity.onActivityResult].
     * It has set resultCode to [Activity.RESULT_OK] with same requestCode, if successfully
     * The requested image savepath has to be stored at caller side (not contained in intent),
     * it can be retrieved using [extractResultFromActivityResult]
     * returns null if an error happened.
     *
     * @param target Path to file to write to, if folder the filename gets app_name + millis + random filename. If null DCIM folder is used.
     */
    @SuppressWarnings("RegExpRedundantEscape")
    fun requestCameraPicture(target: File?): String? {
        if (_context !is Activity) {
            throw RuntimeException("Error: ShareUtil.requestCameraPicture needs an Activity Context.")
        }
        var cameraPictureFilepath: String? = null
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(_context!!.packageManager) != null) {
            val photoFile: File
            try {
                // Create an image file name
                if (target != null && !target.isDirectory) {
                    photoFile = target
                } else {
                    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss", Locale.ENGLISH)
                    val storageDir = 
                        target ?: File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera")
                    val imageFileName =
                        (ContextUtils(_context).rstr("app_name").replace("[^a-zA-Z0-9\.\-]".toRegex(), "_") + "_")
                            .replace("__", "_") + sdf.format(Date())
                    photoFile = File(storageDir, "$imageFileName.jpg")
                    if (!photoFile.parentFile.exists() && !photoFile.parentFile.mkdirs()) {
                        photoFile = File.createTempFile(imageFileName + "_", ".jpg", storageDir)
                    }
                }

                //noinspection StatementWithEmptyBody
                if (!photoFile.parentFile.exists() && photoFile.parentFile.mkdirs());

                // Save a file: path for use with ACTION_VIEW intents
                cameraPictureFilepath = photoFile.absolutePath
            } catch (ex: IOException) {
                return null
            }

            // Continue only if the File was successfully created
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val uri = FileProvider.getUriForFile(_context!!, fileProviderAuthority, photoFile)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
            } else {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile))
            }
            (_context as Activity).startActivityForResult(takePictureIntent, REQUEST_CAMERA_PICTURE)
        }
        _lastCameraPictureFilepath = cameraPictureFilepath
        return cameraPictureFilepath
    }

    /**
     * Extract result data from [Activity.onActivityResult].
     * Forward all arguments from activity. Only requestCodes from [ShareUtil] get analyzed.
     * Also may forward results via local broadcast
     */
    @SuppressLint("ApplySharedPref")
    fun extractResultFromActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        vararg activityOrNull: Activity?
    ): Any? {
        val activity = greedyGetActivity(*activityOrNull) ?: return null
        when (requestCode) {
            REQUEST_CAMERA_PICTURE -> {
                val picturePath = if (resultCode == Activity.RESULT_OK) _lastCameraPictureFilepath else null
                if (picturePath != null) {
                    sendLocalBroadcastWithStringExtra(REQUEST_CAMERA_PICTURE.toString() + "", EXTRA_FILEPATH, picturePath)
                }
                return picturePath
            }
            REQUEST_PICK_PICTURE -> {
                if (resultCode == Activity.RESULT_OK && data != null && data.data != null) {
                    val selectedImage = data.data
                    val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
                    var picturePath: String? = null

                    val cursor = _context!!.contentResolver.query(selectedImage!!, filePathColumn, null, null, null)
                    if (cursor != null && cursor.moveToFirst()) {
                        for (column in filePathColumn) {
                            val curColIndex = cursor.getColumnIndex(column)
                            if (curColIndex == -1) {
                                continue
                            }
                            picturePath = cursor.getString(curColIndex)
                            if (!TextUtils.isEmpty(picturePath)) {
                                break
                            }
                        }
                        cursor.close()
                    }

                    // Try to grab via file extraction method
                    data.action = Intent.ACTION_VIEW
                    picturePath = picturePath ?: extractFileFromIntentStr(data)

                    // Retrieve image from file descriptor / Cloud, e.g.: Google Drive, Picasa
                    if (picturePath == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        try {
                            val parcelFileDescriptor = 
                                _context!!.contentResolver.openFileDescriptor(selectedImage, "r")
                            if (parcelFileDescriptor != null) {
                                val fileDescriptor = parcelFileDescriptor.fileDescriptor
                                val input = FileInputStream(fileDescriptor)

                                // Create temporary file in cache directory
                                picturePath = 
                                    File.createTempFile("image", "tmp", _context!!.cacheDir).absolutePath
                                FileUtils.writeFile(File(picturePath), FileUtils.readCloseBinaryStream(input))
                            }
                        } catch (ignored: IOException) { 
                            // nothing we can do here, null value will be handled below
                        }
                    }

                    // Return path to picture on success, else null
                    if (picturePath != null) {
                        sendLocalBroadcastWithStringExtra(REQUEST_CAMERA_PICTURE.toString() + "", EXTRA_FILEPATH, picturePath)
                    }
                    return picturePath
                }
            }
            REQUEST_SAF -> {
                if (resultCode == Activity.RESULT_OK && data != null && data.data != null) {
                    val treeUri = data.data
                    PreferenceManager.getDefaultSharedPreferences(_context).edit()
                        .putString(PREF_KEY__SAF_TREE_URI, treeUri.toString()).commit()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        activity.contentResolver.takePersistableUriPermission(
                            treeUri!!,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        )
                    }
                    return treeUri
                }
            }
        }
        return null
    }

    /**
     * Send a local broadcast (to receive within app), with given action and string-extra+value.
     * This is a convenience method for quickly sending just one thing.
     */
    fun sendLocalBroadcastWithStringExtra(action: String, extra: String, value: CharSequence) {
        val intent = Intent(action)
        intent.putExtra(extra, value)
        LocalBroadcastManager.getInstance(_context!!).sendBroadcast(intent)
    }

    /**
     * Receive broadcast results via a callback method
     *
     * @param callback       Function to call with received [Intent]
     * @param autoUnregister wether or not to automatically unregister receiver after first match
     * @param filterActions  All [IntentFilter] actions to filter for
     * @return The created instance. Has to be unregistered on [Activity] lifecycle events.
     */
    fun receiveResultFromLocalBroadcast(
        callback: Callback.a2<Intent, BroadcastReceiver>,
        autoUnregister: Boolean,
        vararg filterActions: String
    ): BroadcastReceiver {
        val intentFilter = IntentFilter()
        for (filterAction in filterActions) {
            intentFilter.addAction(filterAction)
        }
        val br = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent?) {
                if (intent != null) {
                    if (autoUnregister) {
                        LocalBroadcastManager.getInstance(_context!!).unregisterReceiver(this)
                    }
                    try {
                        callback.callback(intent, this)
                    } catch (ignored: Exception) { 
                    }
                }
            }
        }
        LocalBroadcastManager.getInstance(_context!!).registerReceiver(br, intentFilter)
        return br
    }

    /**
     * Request edit of image (by image editor/viewer - for example to crop image)
     *
     * @param file File that should be edited
     */
    fun requestPictureEdit(file: File) {
        val uri = getUriByFileProviderAuthority(file)
        val flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION

        val intent = Intent(Intent.ACTION_EDIT)
        intent.setDataAndType(uri, "image/*")
        intent.addFlags(flags)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
        intent.putExtra(EXTRA_FILEPATH, file.absolutePath)

        for (resolveInfo in _context!!.packageManager.queryIntentActivities(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )) {
            val packageName = resolveInfo.activityInfo.packageName
            _context!!.grantUriPermission(packageName, uri, flags)
        }
        _context!!.startActivity(Intent.createChooser(intent, null))
    }

    /**
     * Get content://media/ Uri for given file, or null if not indexed
     *
     * @param file Target file
     * @param mode 1 for picture, 2 for video, anything else for other
     * @return Media URI
     */
    fun getMediaUri(file: File, mode: Int): Uri? {
        var uri = MediaStore.Files.getContentUri("external")
        uri = if (mode != 0) (if (mode == 1) MediaStore.Images.Media.EXTERNAL_CONTENT_URI else MediaStore.Video.Media.EXTERNAL_CONTENT_URI) else uri

        var cursor: Cursor? = null
        try {
            cursor = _context!!.contentResolver.query(
                uri,
                arrayOf(MediaStore.Images.Media._ID),
                MediaStore.Images.Media.DATA + "= ?",
                arrayOf(file.absolutePath),
                null
            )
            if (cursor != null && cursor.moveToFirst()) {
                val mediaid = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media._ID))
                return Uri.withAppendedPath(uri, mediaid.toString() + "")
            }
        } catch (ignored: Exception) { 
        } finally {
            cursor?.close()
        }
        return null
    }

    /**
     * By default Chrome Custom Tabs only uses Chrome Stable to open links
     * There are also other packages (like Chrome Beta, Chromium, Firefox, ..)
     * which implement the Chrome Custom Tab interface. This method changes
     * the customtab intent to use an available compatible browser, if available.
     */
    fun enableChromeCustomTabsForOtherBrowsers(customTabIntent: Intent?) {
        val checkpkgs = arrayOf(
            "com.android.chrome", "com.chrome.beta", "com.chrome.dev", "com.google.android.apps.chrome", "org.chromium.chrome",
            "org.mozilla.fennec_fdroid", "org.mozilla.firefox", "org.mozilla.firefox_beta", "org.mozilla.fennec_aurora",
            "org.mozilla.klar", "org.mozilla.focus"
        )

        // Get all intent handlers for web links
        val pm = _context!!.packageManager
        val urlIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.example.com"))
        val browsers = ArrayList<String>()
        for (ri in pm.queryIntentActivities(urlIntent, 0)) {
            val i = Intent("android.support.customtabs.action.CustomTabsService")
            i.setPackage(ri.activityInfo.packageName)
            if (pm.resolveService(i, 0) != null) {
                browsers.add(ri.activityInfo.packageName)
            }
        }

        // Check if the user has a "default browser" selected
        val ri = pm.resolveActivity(urlIntent, 0)
        val userDefaultBrowser = ri?.activityInfo?.packageName

        // Select which browser to use out of all installed customtab supporting browsers
        var pkg: String? = null
        if (browsers.isEmpty()) {
            pkg = null
        } else if (browsers.size == 1) {
            pkg = browsers[0]
        } else if (!TextUtils.isEmpty(userDefaultBrowser) && browsers.contains(userDefaultBrowser)) {
            pkg = userDefaultBrowser
        } else {
            for (checkpkg in checkpkgs) {
                if (browsers.contains(checkpkg)) {
                    pkg = checkpkg
                    break
                }
            }
            if (pkg == null && browsers.isNotEmpty()) {
                pkg = browsers[0]
            }
        }
        if (pkg != null && customTabIntent != null) {
            customTabIntent.setPackage(pkg)
        }
    }

    /*** 
     * Request storage access. The user needs to press "Select storage" at the correct storage.
     * @param activity The activity which will receive the result from startActivityForResult
     */
    fun requestStorageAccessFramework(vararg activity: Activity?) {
        val a = greedyGetActivity(*activity) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
            intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION 
                        or Intent.FLAG_GRANT_WRITE_URI_PERMISSION 
                        or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION 
                        or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            )
            a.startActivityForResult(intent, REQUEST_SAF)
        }
    }

    /**
     * Get storage access framework tree uri. The user must have granted access via [requestStorageAccessFramework]
     *
     * @return Uri or null if not granted yet
     */
    val storageAccessFrameworkTreeUri: Uri?
        get() {
            val treeStr = PreferenceManager.getDefaultSharedPreferences(_context).getString(PREF_KEY__SAF_TREE_URI, null)
            if (!TextUtils.isEmpty(treeStr)) {
                try {
                    return Uri.parse(treeStr)
                } catch (ignored: Exception) { 
                }
            }
            return null
        }

    /**
     * Get mounted storage folder root (by tree uri). The user must have granted access via [requestStorageAccessFramework]
     *
     * @return File or null if SD not mounted
     */
    val storageAccessFolder: File?
        get() {
            val safUri = storageAccessFrameworkTreeUri
            if (safUri != null) {
                val safUriStr = safUri.toString()
                val cu = ContextUtils(_context)
                for (storage in cu.getStorages(false, true)) {
                    @SuppressWarnings("ConstantConditions") val storageFolderName = storage.first.name
                    if (safUriStr.contains(storageFolderName)) {
                        cu.freeContextRef()
                        return storage.first
                    }
                }
                cu.freeContextRef()
            }
            return null
        }

    /**
     * Check whether or not a file is under a storage access folder (external storage / SD)
     *
     * @param file The file object (file/folder)
     * @return Wether or not the file is under storage access folder
     */
    fun isUnderStorageAccessFolder(file: File?): Boolean {
        if (file != null) {
            // When file writeable as is, it's the fastest way to learn SAF isn't required
            if (file.canWrite()) {
                return false
            }
            val cu = ContextUtils(_context)
            for (storage in cu.getStorages(false, true)) {
                if (file.absolutePath.startsWith(storage.first.absolutePath)) {
                    cu.freeContextRef()
                    return true
                }
            }
            cu.freeContextRef()
        }
        return false
    }

    /**
     * Greedy extract Activity from parameter or convert context if it's a activity
     */
    private fun greedyGetActivity(vararg activity: Activity?): Activity? {
        if (activity.isNotEmpty() && activity[0] != null) {
            return activity[0]
        }
        return if (_context is Activity) {
            _context as Activity
        } else null
    }

    /**
     * Check whether or not a file can be written.
     * Requires storage access framework permission for external storage (SD)
     *
     * @param file  The file object (file/folder)
     * @param isDir Wether or not the given file parameter is a directory
     * @return Wether or not the file can be written
     */
    fun canWriteFile(file: File?, isDir: Boolean): Boolean {
        if (file == null) {
            return false
        } else if (file.absolutePath.startsWith(Environment.getExternalStorageDirectory().absolutePath)
            || file.absolutePath.startsWith(_context!!.filesDir.absolutePath)
        ) {
            val s1 = isDir && file.parentFile.canWrite()
            return if (!isDir && file.parentFile != null) file.parentFile.canWrite() else file.canWrite()
        } else {
            val dof = getDocumentFile(file, isDir)
            return dof != null && dof.canWrite()
        }
    }

    /**
     * Get a [DocumentFile] object out of a normal java [File].
     * When used on a external storage (SD), use [requestStorageAccessFramework]
     * first to get access. Otherwise this will fail.
     *
     * @param file  The file/folder to convert
     * @param isDir Wether or not file is a directory. For non-existing (to be created) files this info is not known hence required.
     * @return A [DocumentFile] object or null if file cannot be converted
     */
    @SuppressWarnings("RegExpRedundantEscape")
    fun getDocumentFile(file: File, isDir: Boolean): DocumentFile? {
        // On older versions use fromFile
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            return DocumentFile.fromFile(file)
        }

        // Get ContextUtils to find storageRootFolder
        val cu = ContextUtils(_context)
        val baseFolderFile = cu.getStorageRootFolder(file)
        cu.freeContextRef()

        val baseFolder = baseFolderFile?.absolutePath
        var originalDirectory = false
        if (baseFolder == null) {
            return null
        }

        var relPath: String? = null
        try {
            val fullPath = file.canonicalPath
            if (baseFolder != fullPath) {
                relPath = fullPath.substring(baseFolder.length + 1)
            } else {
                originalDirectory = true
            }
        } catch (e: IOException) {
            return null
        } catch (ignored: Exception) {
            originalDirectory = true
        }
        val treeUri: Uri = storageAccessFrameworkTreeUri ?: return null
        var dof = DocumentFile.fromTreeUri(_context!!, treeUri)
        if (originalDirectory) {
            return dof
        }
        val parts = relPath!!.split("\/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (i in parts.indices) {
            var nextDof = dof!!.findFile(parts[i])
            if (nextDof == null) {
                try {
                    nextDof = if (i < parts.size - 1 || isDir) dof.createDirectory(parts[i]) else dof.createFile(
                        "image",
                        parts[i]
                    )
                } catch (ignored: Exception) {
                    nextDof = null
                }
            }
            dof = nextDof
        }
        return dof
    }

    fun showMountSdDialog(
        @StringRes title: Int,
        @StringRes description: Int,
        @DrawableRes mountDescriptionGraphic: Int,
        vararg activityOrNull: Activity?
    ) {
        val activity = greedyGetActivity(*activityOrNull) ?: return

        // Image viewer
        val imv = ImageView(activity)
        imv.setImageResource(mountDescriptionGraphic)
        imv.adjustViewBounds = true

        val dialog = AlertDialog.Builder(activity)
        dialog.setView(imv)
        dialog.setTitle(title)
        dialog.setMessage(_context!!.getString(description) + "\n\n")
        dialog.setNegativeButton(android.R.string.cancel, null)
        dialog.setPositiveButton(android.R.string.yes) { _, _ -> requestStorageAccessFramework(activity) }
        val dialogi = dialog.create()
        dialogi.show()
    }

    @SuppressWarnings("ResultOfMethodCallIgnored", "StatementWithEmptyBody")
    fun writeFile(
        file: File,
        isDirectory: Boolean,
        writeFileCallback: Callback.a2<Boolean, FileOutputStream>?
    ) {
        try {
            var fileOutputStream: FileOutputStream? = null
            var pfd: ParcelFileDescriptor? = null
            val existingEmptyFile = file.canWrite() && file.length() < MIN_OVERWRITE_LENGTH
            val nonExistingCreatableFile = !file.exists() && file.parentFile.canWrite()
            if (existingEmptyFile || nonExistingCreatableFile) {
                if (isDirectory) {
                    file.mkdirs()
                } else {
                    fileOutputStream = FileOutputStream(file)
                }
            } else {
                val dof = getDocumentFile(file, isDirectory)
                if (dof != null && dof.uri != null && dof.canWrite()) {
                    if (isDirectory) {
                        // Nothing to do
                    } else {
                        pfd = _context!!.contentResolver.openFileDescriptor(dof.uri, "rwt")
                        fileOutputStream = FileOutputStream(pfd!!.fileDescriptor)
                    }
                }
            }
            writeFileCallback?.callback(fileOutputStream != null || isDirectory && file.exists(), fileOutputStream)
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close()
                } catch (ignored: Exception) { 
                }
            }
            pfd?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Call telephone number.
     * Non direct call, opens up the dialer and pre-sets the telephone number. User needs to press manually.
     * Direct call requires M permission granted, also add permissions to manifest:
     * <uses-permission android:name="android.permission.CALL_PHONE"></uses-permission>
     *
     * @param telNo      The telephone number to call
     * @param directCall Direct call number if possible
     */
    fun callTelephoneNumber(telNo: String, vararg directCall: Boolean) {
        val activity = greedyGetActivity()
            ?: throw RuntimeException("Error: ShareUtil::callTelephoneNumber needs to be contstructed with activity context")
        var ldirectCall = directCall.isNotEmpty() && directCall[0]


        if (Build.VERSION.SDK_INT >= 23 && ldirectCall) {
            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.CALL_PHONE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.CALL_PHONE), 4001)
                ldirectCall = false
            } else {
                try {
                    val callIntent = Intent(Intent.ACTION_CALL)
                    callIntent.data = Uri.parse("tel:$telNo")
                    activity.startActivity(callIntent)
                } catch (ignored: Exception) {
                    ldirectCall = false
                }
            }
        }
        // Show dialer up with telephone number pre-inserted
        if (!ldirectCall) {
            val intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", telNo, null))
            activity.startActivity(intent)
        }
    }

    companion object {
        const val EXTRA_FILEPATH = "real_file_path_2"
        val SDF_RFC3339_ISH = SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss", Locale.getDefault())
        val SDF_SHORT = SimpleDateFormat("yyMMdd-HHmmss", Locale.getDefault())
        val SDF_IMAGES = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()) //20190511-230845
        const val MIME_TEXT_PLAIN = "text/plain"
        const val PREF_KEY__SAF_TREE_URI = "pref_key__saf_tree_uri"

        const val REQUEST_CAMERA_PICTURE = 50001
        const val REQUEST_PICK_PICTURE = 50002
        const val REQUEST_SAF = 50003

        const val MIN_OVERWRITE_LENGTH = 5

        protected var _lastCameraPictureFilepath: String? = null
        protected var _fileProviderAuthority: String? = null

        fun setFileProviderAuthority(fileProviderAuthority: String) {
            _fileProviderAuthority = fileProviderAuthority
        }

        /**
         * Generate a filename based off current datetime in filename (year, month, day, hour, minute, second)
         * Examples: Screenshot_20210208-184301_Trebuchet.png IMG_20190511-230845.jpg
         *
         * @param A0prefixA1postfixA2ext All arguments are optional and default values are taken for null
         * [0] = Prefix [Screenshot/IMG]
         * [1] = Postfix [Trebuchet]
         * [2] = File extensions [jpg/png/txt]
         * @return Filename
         */
        fun getFilenameWithTimestamp(vararg A0prefixA1postfixA2ext: String?): String {
            val prefix =
                (((if (A0prefixA1postfixA2ext.isNotEmpty() && !TextUtils.isEmpty(A0prefixA1postfixA2ext[0])) A0prefixA1postfixA2ext[0] else "Screenshot") + "_").trim { it <= ' ' }
                    .replaceFirst("^_$".toRegex(), ""))
            val postfix =
                ("_") + (if (A0prefixA1postfixA2ext.size > 1 && !TextUtils.isEmpty(A0prefixA1postfixA2ext[1])) A0prefixA1postfixA2ext[1] else "")).trim { it <= ' ' }
                    .replaceFirst("^_$".toRegex(), "")
            val ext =
                if (A0prefixA1postfixA2ext.size > 2 && !TextUtils.isEmpty(A0prefixA1postfixA2ext[2])) A0prefixA1postfixA2ext[2] else "jpg"
            return String.format(
                "%s%s%s.%s",
                prefix.trim { it <= ' ' },
                SDF_IMAGES.format(Date()),
                postfix.trim { it <= ' ' },
                ext!!.toLowerCase().replace(".", "").replace("jpeg", "jpg")
            )
        }

        /**
         * Create a picture out of [WebView]'s whole content
         *
         * @param webView The WebView to get contents from
         * @return A [Bitmap] or null
         */
        fun getBitmapFromWebView(webView: WebView, vararg a0fullpage: Boolean): Bitmap? {
            try {
                //Measure WebView's content
                if (a0fullpage.isNotEmpty() && a0fullpage[0]) {
                    val widthMeasureSpec =
                        View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
                    val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                    webView.measure(widthMeasureSpec, heightMeasureSpec)
                    webView.layout(0, 0, webView.measuredWidth, webView.measuredHeight)
                }

                //Build drawing cache and store its size
                webView.buildDrawingCache()

                //Creates the bitmap and draw WebView's content on in
                val bitmap = 
                    Bitmap.createBitmap(webView.measuredWidth, webView.measuredHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawBitmap(bitmap, 0f, bitmap.height.toFloat(), Paint())

                webView.draw(canvas)
                webView.destroyDrawingCache()

                return bitmap
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            } catch (e: OutOfMemoryError) {
                e.printStackTrace()
                return null
            }
        }
    }
}
