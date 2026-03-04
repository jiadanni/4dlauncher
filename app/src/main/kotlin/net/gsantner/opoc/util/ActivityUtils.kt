/*#######################################################
 *
 *   Maintained 2016-2023 by Gregor Santner <gsantner AT mailbox DOT org>
 *
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *     https://github.com/gsantner/opoc/#licensing
 *
#########################################################*/
package net.gsantner.opoc.util

import android.app.Activity
import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.CalendarContract
import android.text.Html
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.webkit.WebView
import android.widget.ScrollView
import androidx.annotation.ColorInt
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatTextView
import com.google.android.material.snackbar.Snackbar


@Suppress(
    "WeakerAccess",
    "unused",
    "SameParameterValue",
    "SpellCheckingInspection",
    "rawtypes",
    "UnusedReturnValue"
)
class ActivityUtils(activity: Activity) : ContextUtils(activity) {
    //########################
    //## Members, Constructors
    //########################
    protected var _activity: Activity? = activity

    override fun freeContextRef() {
        super.freeContextRef()
        _activity = null
    }

    //########################
    //##     Methods
    //########################

    /**
     * Animate to specified Activity
     *
     * @param to                 The class of the activity
     * @param finishFromActivity true: Finish the current activity
     * @param requestCode        Request code for stating the activity, not waiting for result if null
     */
    fun animateToActivity(to: Class<*>, finishFromActivity: Boolean?, requestCode: Int?) {
        animateToActivity(Intent(_activity, to), finishFromActivity, requestCode)
    }

    /**
     * Animate to Activity specified in intent
     * Requires animation resources
     *
     * @param intent             Intent to open start an activity
     * @param finishFromActivity true: Finish the current activity
     * @param requestCode        Request code for stating the activity, not waiting for result if null
     */
    fun animateToActivity(intent: Intent, finishFromActivity: Boolean?, requestCode: Int?) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        if (requestCode != null) {
            _activity!!.startActivityForResult(intent, requestCode)
        } else {
            _activity!!.startActivity(intent)

        }
        _activity!!.overridePendingTransition(getResId(ResType.DIMEN, "fadein"), getResId(ResType.DIMEN, "fadeout"))
        if (finishFromActivity != null && finishFromActivity) {
            _activity!!.finish()
        }
    }


    fun showSnackBar(@StringRes stringResId: Int, showLong: Boolean): Snackbar {
        val s = Snackbar.make(
            _activity!!.findViewById(android.R.id.content), stringResId,
            if (showLong) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT
        )
        s.show()
        return s
    }

    fun showSnackBar(
        @StringRes stringResId: Int,
        showLong: Boolean,
        @StringRes actionResId: Int,
        listener: View.OnClickListener
    ) {
        Snackbar.make(
            _activity!!.findViewById(android.R.id.content), stringResId,
            if (showLong) Snackbar.LENGTH_LONG else Snackbar.LENGTH_SHORT
        )
            .setAction(actionResId, listener)
            .show()
    }

    fun setSoftKeyboardVisibile(visible: Boolean, vararg editView: View): ActivityUtils {
        val activity = _activity
        if (activity != null) {
            val v =
                if (editView.isNotEmpty()) editView[0] else if (activity.currentFocus != null && activity.currentFocus!!.windowToken != null) activity.currentFocus else null
            val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
            if (v != null && imm != null) {
                val r = Runnable {
                    if (visible) {
                        v.requestFocus()
                        imm.showSoftInput(v, InputMethodManager.SHOW_FORCED)
                    } else {
                        imm.hideSoftInputFromWindow(v.windowToken, 0)
                    }
                }
                r.run()
                for (d in intArrayOf(100, 350)) {
                    v.postDelayed(r, d.toLong())
                }
            }
        }
        return this
    }

    fun hideSoftKeyboard(): ActivityUtils {
        if (_activity != null) {
            val imm = _activity!!.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
            if (imm != null && _activity!!.currentFocus != null && _activity!!.currentFocus!!.windowToken != null) {
                imm.hideSoftInputFromWindow(_activity!!.currentFocus!!.windowToken, 0)
            }
        }
        return this
    }

    fun showSoftKeyboard(): ActivityUtils {
        if (_activity != null) {
            val imm = _activity!!.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
            if (imm != null && _activity!!.currentFocus != null && _activity!!.currentFocus!!.windowToken != null) {
                showSoftKeyboard(_activity!!.currentFocus!!)
            }
        }
        return this
    }


    fun showSoftKeyboard(textInputView: View): ActivityUtils {
        if (_activity != null) {
            val imm = _activity!!.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager?
            imm?.showSoftInput(textInputView, InputMethodManager.SHOW_FORCED)
        }
        return this
    }

    fun showDialogWithHtmlTextView(@StringRes resTitleId: Int, html: String) {
        showDialogWithHtmlTextView(resTitleId, html, true, null)
    }

    fun showDialogWithHtmlTextView(
        @StringRes resTitleId: Int,
        text: String,
        isHtml: Boolean,
        dismissedListener: DialogInterface.OnDismissListener?
    ) {
        val scroll = ScrollView(_context)
        val textView = AppCompatTextView(_context!!)
        val padding =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16f, _context!!.resources.displayMetrics).toInt()

        scroll.setPadding(padding, 0, padding, 0)
        scroll.addView(textView)
        textView.movementMethod = LinkMovementMethod()
        textView.text = if (isHtml) SpannableString(Html.fromHtml(text)) else text
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17f)

        val dialog = AlertDialog.Builder(_context!!)
            .setPositiveButton(android.R.string.ok, null).setOnDismissListener(dismissedListener)
            .setView(scroll)
        if (resTitleId != 0) {
            dialog.setTitle(resTitleId)
        }
        dialogFullWidth(dialog.show(), true, false)
    }

    fun showDialogWithRawFileInWebView(fileInRaw: String, @StringRes resTitleId: Int) {
        val wv = WebView(_context!!)
        wv.loadUrl("file:///android_res/raw/$fileInRaw")
        val dialog = AlertDialog.Builder(_context!!)
            .setPositiveButton(android.R.string.ok, null)
            .setTitle(resTitleId)
            .setView(wv)
        dialogFullWidth(dialog.show(), true, false)
    }

    // Toggle with no param, else set visibility according to first bool
    fun toggleStatusbarVisibility(vararg optionalForceVisible: Boolean): ActivityUtils {
        val attrs = _activity!!.window.attributes
        val flag = WindowManager.LayoutParams.FLAG_FULLSCREEN
        if (optionalForceVisible.isEmpty()) {
            attrs.flags = attrs.flags xor flag
        } else if (optionalForceVisible.size == 1 && optionalForceVisible[0]) {
            attrs.flags = attrs.flags and flag.inv()
        } else {
            attrs.flags = attrs.flags or flag
        }
        _activity!!.window.attributes = attrs
        return this
    }

    fun showGooglePlayEntryForThisApp(): ActivityUtils {
        val pkgId = "details?id=" + _activity!!.packageName
        val goToMarket = Intent(Intent.ACTION_VIEW, Uri.parse("market://$pkgId"))
        goToMarket.addFlags(
            Intent.FLAG_ACTIVITY_NO_HISTORY or
                    (if (Build.VERSION.SDK_INT >= 21) Intent.FLAG_ACTIVITY_NEW_DOCUMENT else Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET) or
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK
        )
        try {
            _activity!!.startActivity(goToMarket)
        } catch (e: ActivityNotFoundException) {
            _activity!!.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/$pkgId")
                )
            )
        }
        return this
    }

    fun setStatusbarColor(color: Int, vararg fromRes: Boolean): ActivityUtils {
        var color = color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (fromRes.isNotEmpty() && fromRes[0]) {
                color = ContextCompat.getColor(_context!!, color)
            }

            _activity!!.window.statusBarColor = color
        }
        return this
    }

    fun setLauncherActivityEnabled(activityClass: Class<*>, enable: Boolean): ActivityUtils {
        try {
            val component = ComponentName(_context!!, activityClass)
            _context!!.packageManager.setComponentEnabledSetting(
                component,
                if (enable) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        } catch (ignored: Exception) {
        }
        return this
    }

    fun isLauncherEnabled(activityClass: Class<*>): Boolean {
        try {
            val component = ComponentName(_context!!, activityClass)
            return _context!!.packageManager.getComponentEnabledSetting(component) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        } catch (ignored: Exception) {
        }
        return false
    }

    @get:ColorInt
    val currentPrimaryColor: Int?
        get() {
            val typedValue = TypedValue()
            _context!!.theme.resolveAttribute(getResId(ResType.ATTR, "colorPrimary"), typedValue, true)
            return typedValue.data
        }

    @get:ColorInt
    val currentPrimaryDarkColor: Int?
        get() {
            val typedValue = TypedValue()
            _context!!.theme.resolveAttribute(getResId(ResType.ATTR, "colorPrimaryDark"), typedValue, true)
            return typedValue.data
        }

    @get:ColorInt
    val currentAccentColor: Int?
        get() {
            val typedValue = TypedValue()
            _context!!.theme.resolveAttribute(getResId(ResType.ATTR, "colorAccent"), typedValue, true)
            return typedValue.data
        }

    @get:ColorInt
    val activityBackgroundColor: Int
        get() {
            val array = _activity!!.theme.obtainStyledAttributes(
                intArrayOf(
                    android.R.attr.colorBackground
                )
            )
            val c = array.getColor(0, -0x1000000)
            array.recycle()
            return c
        }

    fun startCalendarApp(): ActivityUtils {
        val builder = CalendarContract.CONTENT_URI.buildUpon()
        builder.appendPath("time")
        builder.appendPath(System.currentTimeMillis().toString())
        val intent = Intent(Intent.ACTION_VIEW, builder.build())
        _activity!!.startActivity(intent)
        return this
    }

    /**
     * Detect if the activity is currently in splitscreen/multiwindow mode
     */
    val isInSplitScreenMode: Boolean
        get() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return _activity!!.isInMultiWindowMode
            }
            return false
        }

    /**
     * Show dialog in full width / show keyboard
     *
     * @param dialog Get via dialog.show()
     */
    fun dialogFullWidth(dialog: AlertDialog?, fullWidth: Boolean, showKeyboard: Boolean) {
        try {
            val w = dialog?.window
            if (w != null) {
                if (fullWidth) {
                    w.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT)
                }
                if (showKeyboard) {
                    w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                }
            }
        } catch (ignored: Exception) {
        }
    }

    // Make activity/app not show up in the recents history - call before finish / System.exit
    fun removeActivityFromHistory(): ActivityUtils {
        try {
            val am = _activity!!.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val tasks = am.appTasks
                if (tasks.isNotEmpty()) {
                    tasks[0].setExcludeFromRecents(true)
                }
            }

        } catch (ignored: Exception) {
        }
        return this
    }
}
