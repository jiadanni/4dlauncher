/*#######################################################
 *
 *   Maintained 2018-2023 by Gregor Santner <gsantner AT mailbox DOT org>
 *
 *   License: Apache 2.0
 *  https://github.com/gsantner/opoc/#licensing
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
/*
 * Add dependencies:
    implementation "com.android.support:preference-v7:${version_library_appcompat}"
    implementation "com.android.support:preference-v14:${version_library_appcompat}"

 * Apply to activity using setTheme(), add to styles.xml/theme:
        <item name="preferenceTheme">@style/PreferenceThemeOverlay.v14.Material</item>
 * OR
    <style name="AppTheme" ...
        <item name="preferenceTheme">@style/AppTheme.PreferenceTheme</item>
    </style>
    <style name="AppTheme.PreferenceTheme" parent="PreferenceThemeOverlay.v14.Material">
      <item name="preferenceCategoryStyle">@style/AppTheme.PreferenceTheme.CategoryStyle</item>
    </style>
    <style name="AppTheme.PreferenceTheme.CategoryStyle" parent="Preference.Category">
        <item name="android:layout">@layout/opoc_pref_category_text</item>
    </style>

 * Layout file:
    <?xml version="1.0" encoding="utf-8"?>
    <TextView xmlns:android="http://schemas.android.com/apk/res/android"
        android:id="@android:id/title"
        style="?android:attr/listSeparatorTextViewStyle"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textAllCaps="false"
        android:textColor="@color/colorAccent" />


 */
package net.gsantner.opoc.preference

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.annotation.XmlRes
import androidx.fragment.app.Fragment
import androidx.preference.DialogPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceScreen
import androidx.recyclerview.widget.RecyclerView
import net.gsantner.opoc.util.ActivityUtils
import net.gsantner.opoc.util.Callback
import net.gsantner.opoc.util.ContextUtils

/**
 * Baseclass to use as preference fragment (with support libraries)
 */
@Suppress("WeakerAccess", "unused", "UnusedReturnValue")
abstract class GsPreferenceFragmentCompat<AS : SharedPreferencesPropertyBackend> : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener, PreferenceFragmentCompat.OnPreferenceStartScreenCallback {
    private var _isDividerVisible = false

    //
    // Abstract
    //

    @get:XmlRes
    abstract val preferenceResourceForInflation: Int

    abstract val fragmentTag: String

    protected abstract fun getAppSettings(context: Context): AS

    //
    // Virtual
    //

    open fun onPreferenceClicked(preference: Preference, key: String, keyResId: Int): Boolean? {
        return null
    }

    open val sharedPreferencesName: String
        get() = "app"

    protected open fun afterOnCreate(savedInstances: Bundle?, context: Context) {

    }

    open fun doUpdatePreferences() {}

    protected open fun onPreferenceScreenChanged(
        preferenceFragmentCompat: PreferenceFragmentCompat,
        preferenceScreen: PreferenceScreen
    ) {
    }

    open val iconTintColor: Int?
        get() = _defaultIconTintColor

    open val title: String?
        get() = null

    //
    //
    //

    private val _registeredPrefs = HashSet<String>()
    private val _prefScreenBackstack = ArrayList<PreferenceScreen>()
    protected lateinit var _appSettings: AS
    protected var _defaultIconTintColor: Int = 0
    protected lateinit var _cu: ContextUtils

    @Deprecated("")
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val activity = activity
        _appSettings = getAppSettings(activity!!)
        _cu = ContextUtils(activity)
        preferenceManager.sharedPreferencesName = sharedPreferencesName
        addPreferencesFromResource(preferenceResourceForInflation)

        if (activity.theme != null) {
            val array = activity.theme.obtainStyledAttributes(intArrayOf(android.R.attr.colorBackground))
            val bgcolor = array.getColor(0, -0x1)
            _defaultIconTintColor = if (_cu.shouldColorOnTopBeLight(bgcolor)) Color.WHITE else Color.BLACK
        }

        // on bottom
        afterOnCreate(savedInstanceState, activity)
    }

    val updatePreferenceIcons: Callback.a1<PreferenceFragmentCompat> = object : Callback.a1<PreferenceFragmentCompat> {
        override fun callback(frag: PreferenceFragmentCompat) {
            try {
                val view = frag.view
                val color = iconTintColor
                if (view != null && color != null) {
                    val r = { tintAllPrefIcons(frag, color) }
                    for (delayFactor in intArrayOf(1, 10, 50, 100, 500)) {
                        view.postDelayed(r, (delayFactor * DEFAULT_ICON_TINT_DELAY).toLong())
                    }
                }
            } catch (ignored: Exception) {
            }
        }
    }

    fun tintAllPrefIcons(preferenceFragment: PreferenceFragmentCompat, @ColorInt iconColor: Int) {
        tintPrefIconsRecursive(preferenceScreen, iconColor)
    }

    private fun tintPrefIconsRecursive(prefGroup: PreferenceGroup?, @ColorInt iconColor: Int) {
        if (prefGroup != null && isAdded) {
            val prefCount = prefGroup.preferenceCount
            for (i in 0 until prefCount) {
                val pref = prefGroup.getPreference(i)
                if (pref != null) {
                    if (isAllowedToTint(pref)) {
                        pref.icon = _cu.tintDrawable(pref.icon, iconColor)
                    }
                    if (pref is PreferenceGroup) {
                        tintPrefIconsRecursive(pref, iconColor)
                    }
                }
            }
        }
    }

    protected open fun isAllowedToTint(pref: Preference): Boolean {
        return true
    }

    /**
     * Try to fetch string resource id from key
     * This only works if the key is only defined once and value=key
     */
    protected fun keyToStringResId(preference: Preference?): Int {
        return if (preference != null && !TextUtils.isEmpty(preference.key)) {
            _cu.getResId(ContextUtils.ResType.STRING, preference.key)
        } else 0
    }

    /**
     * Try to fetch string resource id from key
     * This only works if the key is only defined once and value=key
     */
    protected fun keyToStringResId(keyAsString: String): Int {
        return _cu.getResId(ContextUtils.ResType.STRING, keyAsString)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updatePreferenceIcons.callback(this)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            view.postDelayed({
                val lpg = view.layoutParams
                if (lpg is LinearLayout.LayoutParams) {
                    val lp = lpg
                    lp.rightMargin = _cu.convertDpToPx(16f).toInt()
                    lp.leftMargin = lp.rightMargin
                    view.layoutParams = lp
                } else if (lpg is FrameLayout.LayoutParams) {
                    val lp = lpg
                    lp.rightMargin = _cu.convertDpToPx(16f).toInt()
                    lp.leftMargin = lp.rightMargin
                    view.layoutParams = lp
                } else if (lpg is RelativeLayout.LayoutParams) {
                    val lp = lpg
                    lp.rightMargin = _cu.convertDpToPx(16f).toInt()
                    lp.leftMargin = lp.rightMargin
                    view.layoutParams = lp
                }
            }, 10)
        }
    }

    @Synchronized
    private fun updatePreferenceChangedListeners(shouldListen: Boolean) {
        val tprefname = sharedPreferencesName
        if (shouldListen && tprefname != null && !_registeredPrefs.contains(tprefname)) {
            val preferences = _appSettings.context.getSharedPreferences(tprefname, Context.MODE_PRIVATE)
            _appSettings.registerPreferenceChangedListener(preferences, this)
            _registeredPrefs.add(tprefname)
        } else if (!shouldListen) {
            for (prefname in _registeredPrefs) {
                val preferences = _appSettings.context.getSharedPreferences(prefname, Context.MODE_PRIVATE)
                _appSettings.unregisterPreferenceChangedListener(preferences, this)
            }
        }
    }


    override fun onResume() {
        super.onResume()
        updatePreferenceChangedListeners(true)
        doUpdatePreferences() // Invoked later
        onPreferenceScreenChangedPriv(this, preferenceScreen)
    }

    override fun onPause() {
        super.onPause()
        updatePreferenceChangedListeners(false)
    }

    @Deprecated("")
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (isAdded) {
            onPreferenceChanged(sharedPreferences, key)
            doUpdatePreferences()
        }
    }

    protected open fun onPreferenceChanged(prefs: SharedPreferences?, key: String?) {
        // Wait some ms to be sure the pref objects have changed it's internal values
        // and the new values are ready to be read ;)
        val r = { doUpdatePreferences() }
        view?.postDelayed(r, 350) ?: r()
    }

    @Deprecated("")
    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        if (isAdded) {
            val key = if (preference.hasKey()) preference.key else ""
            val keyResId = keyToStringResId(preference)
            val ret = onPreferenceClicked(preference, key, keyResId)
            if (ret != null) {
                return ret
            }
        }
        return super.onPreferenceTreeClick(preference)
    }


    override fun getCallbackFragment(): Fragment {
        return this
    }

    override fun onStop() {
        _prefScreenBackstack.clear()
        super.onStop()
    }

    @Deprecated("")
    override fun onPreferenceStartScreen(
        preferenceFragmentCompat: PreferenceFragmentCompat,
        preferenceScreen: PreferenceScreen
    ): Boolean {
        _prefScreenBackstack.add(getPreferenceScreen())
        preferenceFragmentCompat.preferenceScreen = preferenceScreen
        updatePreferenceIcons.callback(this)
        onPreferenceScreenChangedPriv(preferenceFragmentCompat, preferenceScreen)
        return true
    }

    protected fun updateSummary(@StringRes keyResId: Int, summary: CharSequence) {
        updatePreference(keyResId, null, null, summary, null)
    }

    /**
     * Finds a [Preference] based on its key res id.
     *
     * @param key The key of the preference to retrieve.
     * @return The [DialogPreference] with the key, or null.
     * @see android.support.v7.preference.PreferenceGroup.findPreference
     */
    fun setDialogMessage(@StringRes key: Int, message: CharSequence): DialogPreference? {
        val p = findPreference<Preference>(key)
        if (p is DialogPreference) {
            p.dialogMessage = message
            return p
        }
        return null
    }

    /**
     * Finds a [Preference] based on its key res id.
     *
     * @param key The key of the preference to retrieve.
     * @return The [Preference] with the key, or null.
     * @see android.support.v7.preference.PreferenceGroup.findPreference
     */
    fun <T : Preference> findPreference(@StringRes key: Int): T? {
        return if (isAdded) findPreference(getString(key)) else null
    }

    /**
     * Finds a [Preference] based on its key res id.
     *
     * @param key The key of the preference to retrieve.
     * @return The [Preference] with the key, or null.
     * @see android.support.v7.preference.PreferenceGroup.findPreference
     */
    fun setPreferenceVisible(@StringRes key: Int, visible: Boolean): Preference? {
        val pref: Preference?
        if (findPreference<Preference>(key).also { pref = it } != null) {
            pref!!.isVisible = visible
        }
        return pref
    }

    protected fun updatePreference(
        @StringRes keyResId: Int,
        @DrawableRes iconRes: Int?,
        title: CharSequence?,
        summary: CharSequence?,
        visible: Boolean?
    ): Preference? {
        val pref = findPreference<Preference>(getString(keyResId))
        if (pref != null) {
            if (summary != null) {
                pref.summary = summary
            }
            if (title != null) {
                pref.title = title
            }
            if (iconRes != null && iconRes != 0) {
                if (isAllowedToTint(pref)) {
                    pref.icon = _cu.tintDrawable(iconRes, iconTintColor!!)
                } else {
                    pref.setIcon(iconRes)
                }
            }
            if (visible != null) {
                pref.isVisible = visible
            }
        }
        return pref
    }

    protected fun removePreference(preference: Preference?) {
        if (preference == null) {
            return
        }
        val parent = getPreferenceParent(preferenceScreen, preference) ?: return
        parent.removePreference(preference)
    }

    fun canGoBack(): Boolean {
        return _prefScreenBackstack.isNotEmpty()
    }

    fun goBack() {
        if (canGoBack()) {
            val screen = _prefScreenBackstack.removeAt(_prefScreenBackstack.size - 1)
            setPreferenceScreen(screen)
            onPreferenceScreenChangedPriv(this, screen)
        }
    }

    protected fun getPreferenceParent(prefGroup: PreferenceGroup, pref: Preference): PreferenceGroup? {
        for (i in 0 until prefGroup.preferenceCount) {
            val prefChild = prefGroup.getPreference(i)
            if (prefChild === pref) {
                return prefGroup
            }
            if (prefChild is PreferenceGroup) {
                val childGroup = prefChild
                val result = getPreferenceParent(childGroup, pref)
                if (result != null) {
                    return result
                }
            }
        }
        return null
    }

    private fun onPreferenceScreenChangedPriv(
        preferenceFragmentCompat: PreferenceFragmentCompat,
        preferenceScreen: PreferenceScreen
    ) {
        this.isDividerVisible = isDividerVisible
        onPreferenceScreenChanged(preferenceFragmentCompat, preferenceScreen)
        updatePreferenceChangedListeners(true)
        doUpdatePreferences()
    }

    /**
     * Is key equal
     *
     * @param pref     A preference
     * @param resIdKey the resource id of the string
     * @return if equals
     */
    fun eq(pref: Preference?, @StringRes resIdKey: Int): Boolean {
        return pref != null && getString(resIdKey) == pref.key
    }


    /**
     * Is key equal
     *
     * @param key      the key
     * @param resIdKey the resource id of the string
     * @return if equals
     */
    fun eq(key: String?, @StringRes resIdKey: Int): Boolean {
        return getString(resIdKey) == key
    }

    fun hasTitle(): Boolean {
        return !TextUtils.isEmpty(title)
    }

    fun getTitleOrDefault(defaultTitle: String): String {
        return if (hasTitle()) title!! else defaultTitle
    }

    protected fun restartActivity() {
        val currentActivity = activity
        if (isAdded && currentActivity != null) {
            val intent = requireActivity().intent
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
            currentActivity.overridePendingTransition(0, 0)
            currentActivity.finish()
            currentActivity.overridePendingTransition(0, 0)
            startActivity(intent)
        }
    }

    /**
     * Append a pref to given `target`. If target is null, the current screen is taken
     * The pref icon is tint according to color
     *
     * @param pref   Preference to add
     * @param target The target to add the pref to, or null for current screen
     * @return true if successfully added
     */
    protected fun appendPreference(pref: Preference, target: PreferenceGroup?): Boolean {
        var target = target
        if (target == null) {
            target = preferenceScreen ?: return false
        }
        if (iconTintColor != null && pref.icon != null && isAllowedToTint(pref)) {
            pref.icon = _cu.tintDrawable(pref.icon, iconTintColor!!)
        }
        return target.addPreference(pref)
    }


    //###############################
    //### Divider
    ////###############################


    var isDividerVisible: Boolean
        get() = _isDividerVisible
        set(visible) {
            _isDividerVisible = visible
            val recyclerView = listView
            if (visible) {
                recyclerView.addItemDecoration(DividerDecoration(requireContext(), dividerColor, _flatPosIsPreferenceCategoryCallback))
            } else if (recyclerView.itemDecorationCount > 0) {
                recyclerView.removeItemDecorationAt(0)
            }
        }

    val dividerColor: Int?
        get() {
            val au = ActivityUtils(activity!!)
            try {
                return Color.parseColor(if ((au as ContextUtils).shouldColorOnTopBeLight(au.activityBackgroundColor)) "#3d3d3d" else "#d1d1d1")
            } catch (ignored: Exception) {
                return null
            } finally {
                au.freeContextRef()
            }
        }

    internal var _flatPosIsPreferenceCategoryCallback: Callback.b1<Int> = object : Callback.b1<Int> {
        override fun callback(position: Int): Boolean {
            var flatPos = 0
            val prefGroup = preferenceScreen
            if (prefGroup != null) {
                val prefCount = prefGroup.preferenceCount
                for (i in 0 until prefCount) {
                    val pref = prefGroup.getPreference(i)
                    if (pref != null) {
                        if (pref is PreferenceCategory) {
                            val prefSubGroup = pref
                            for (j in 0 until prefSubGroup.preferenceCount) {
                                flatPos++
                                if (flatPos == position) {
                                    return prefSubGroup.getPreference(j) !is PreferenceCategory
                                }
                            }
                        } else if (flatPos == position) {
                            return true
                        }
                    }
                    flatPos++
                }
            }
            return false
        }
    }

    /**
     * Divider for preferences
     */
    class DividerDecoration : RecyclerView.ItemDecoration {
        private val _isCategoryAtFlatpositionCallback: Callback.b1<Int>?
        private val _paint: Paint
        private val _heightDp: Int

        constructor(context: Context, isCategoryAtFlatpos: Callback.b1<Int>?) : this(
            context,
            null,
            1f,
            isCategoryAtFlatpos
        ) {
        }

        // b8b8b8          = default divider color
        // d1d1d1 / 3d3d3d = color for light / dark mode
        constructor(context: Context, @ColorInt color: Int?, isCategoryAtFlatpos: Callback.b1<Int>?) : this(
            context,
            color,
            1f,
            isCategoryAtFlatpos
        ) {
        }

        constructor(
            context: Context,
            @ColorInt color: Int?,
            heightDp: Float,
            isCategoryAtFlatpos: Callback.b1<Int>?
        ) {
            var color = color
            if (color == null) {
                color = Color.parseColor("#b8b8b8")
            }
            _isCategoryAtFlatpositionCallback = isCategoryAtFlatpos
            _paint = Paint()
            _paint.style = Paint.Style.FILL
            _paint.color = color
            _heightDp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, heightDp, context.resources.displayMetrics)
                .toInt()
        }

        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            val position = parent.getChildAdapterPosition(view)
            var viewType = 0
            try {
                viewType = parent.adapter?.getItemViewType(position) ?: 0
            } catch (ignored: NullPointerException) {
            }

            if (viewType != 1) {
                outRect.set(0, 0, 0, _heightDp)
            } else {
                outRect.setEmpty()
            }
        }

        override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            for (i in 0 until parent.childCount) {
                val view = parent.getChildAt(i)
                val position = parent.getChildAdapterPosition(view)
                if (_isCategoryAtFlatpositionCallback == null || _isCategoryAtFlatpositionCallback!!.callback(position)) {
                    c.drawRect(
                        view.left.toFloat(),
                        view.bottom.toFloat(),
                        view.right.toFloat(),
                        (view.bottom + _heightDp).toFloat(),
                        _paint
                    )
                }
            }
        }
    }

    companion object {
        private const val DEFAULT_ICON_TINT_DELAY = 200
    }
}
