/*#######################################################
 * 
 *   Maintained 2016-2023 by Gregor Santner <gsantner @mailbox.org>
 *
 *   License: Apache 2.0
 *  https://github.com/gsantner/opoc/#licensing
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
/*
 * This is a wrapper for settings based on SharedPreferences
 * with keys in resources. Extend from this class and add
 * getters/setters for the app's settings.
 * Example:
    public boolean isAppFirstStart(boolean doSet) {
        int value = getInt(R.string.pref_key__app_first_start, -1);
        if (doSet) {
            setBool(true);
        }
        return value;
    }

    public boolean isAppCurrentVersionFirstStart(boolean doSet) {
        int value = getInt(R.string.pref_key__app_first_start_current_version, -1);
        if (doSet) {
            setInt(R.string.pref_key__app_first_start_current_version, BuildConfig.VERSION_CODE);
        }
        return value != BuildConfig.VERSION_CODE;
    }
 */

package net.gsantner.opoc.preference

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import java.io.File
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar


/**
 * Wrapper for settings based on SharedPreferences, optionally with keys in resources
 * Default SharedPreference (_prefApp) will be taken if no SP is specified, else the first one
 */
@Suppress("WeakerAccess", "unused", "SpellCheckingInspection", "SameParameterValue")
open class SharedPreferencesPropertyBackend(context: Context, prefAppName: String?) :
    PropertyBackend<String, SharedPreferencesPropertyBackend> {
    //
    // Members, Constructors
    //
    protected val _prefApp: SharedPreferences
    protected val _prefAppName: String
    val context: Context

    constructor(context: Context) : this(context, SHARED_PREF_APP)

    init {
        this.context = context.applicationContext
        _prefAppName = if (TextUtils.isEmpty(prefAppName))
            this.context.packageName + "_preferences"
        else
            prefAppName!!
        _prefApp = this.context.getSharedPreferences(_prefAppName, Context.MODE_PRIVATE)
    }

    //
    // Methods
    //
    fun isKeyEqual(key: String, stringKeyResourceId: Int): Boolean {
        return key == rstr(stringKeyResourceId)
    }

    fun resetSettings() {
        resetSettings(_prefApp)
    }

    @SuppressLint("ApplySharedPref")
    fun resetSettings(pref: SharedPreferences) {
        pref.edit().clear().commit()
    }

    fun isPrefSet(@StringRes stringKeyResourceId: Int): Boolean {
        return isPrefSet(_prefApp, stringKeyResourceId)
    }

    fun isPrefSet(pref: SharedPreferences, @StringRes stringKeyResourceId: Int): Boolean {
        return pref.contains(rstr(stringKeyResourceId))
    }

    fun registerPreferenceChangedListener(value: SharedPreferences.OnSharedPreferenceChangeListener) {
        registerPreferenceChangedListener(_prefApp, value)
    }

    fun registerPreferenceChangedListener(
        pref: SharedPreferences,
        value: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        pref.registerOnSharedPreferenceChangeListener(value)
    }

    fun unregisterPreferenceChangedListener(value: SharedPreferences.OnSharedPreferenceChangeListener) {
        unregisterPreferenceChangedListener(_prefApp, value)
    }

    fun unregisterPreferenceChangedListener(
        pref: SharedPreferences,
        value: SharedPreferences.OnSharedPreferenceChangeListener
    ) {
        pref.unregisterOnSharedPreferenceChangeListener(value)
    }

    val defaultPreferences: SharedPreferences
        get() = _prefApp

    val defaultPreferencesEditor: SharedPreferences.Editor
        get() = _prefApp.edit()

    val defaultPreferencesName: String
        get() = _prefAppName


    private fun gp(vararg pref: SharedPreferences): SharedPreferences {
        return if (pref.isNotEmpty()) pref[0] else _prefApp
    }


    //
    // Getter for resources
    //
    fun rstr(@StringRes stringKeyResourceId: Int): String {
        return context.getString(stringKeyResourceId)
    }

    fun rcolor(@ColorRes resColorId: Int): Int {
        return ContextCompat.getColor(context, resColorId)
    }

    fun rstrs(vararg keyResourceIds: Int): Array<String> {
        val ret = Array(keyResourceIds.size) { "" }
        for (i in keyResourceIds.indices) {
            ret[i] = rstr(keyResourceIds[i])
        }
        return ret
    }


    //
    // Getter & Setter for String
    //
    fun setString(@StringRes keyResourceId: Int, value: String, vararg pref: SharedPreferences) {
        gp(*pref).edit().putString(rstr(keyResourceId), value).apply()
    }

    fun setString(key: String, value: String, vararg pref: SharedPreferences): SharedPreferencesPropertyBackend {
        var finalValue = value
        if (key == context.getString(com.jiadanni.launcher4d.R.string.pref_key__feed_weather_api_key)) {
            finalValue = com.jiadanni.launcher4d.util.CryptoUtils.encrypt(value)
        }
        gp(*pref).edit().putString(key, finalValue).apply()
        return this
    }

    fun setString(@StringRes keyResourceId: Int, @StringRes defaultValueResourceId: Int, vararg pref: SharedPreferences) {
        gp(*pref).edit().putString(rstr(keyResourceId), rstr(defaultValueResourceId)).apply()
    }

    fun getString(@StringRes keyResourceId: Int, defaultValue: String, vararg pref: SharedPreferences): String {
        return gp(*pref).getString(rstr(keyResourceId), defaultValue)!!
    }

    fun getString(
        @StringRes keyResourceId: Int,
        @StringRes defaultValueResourceId: Int,
        vararg pref: SharedPreferences
    ): String {
        return gp(*pref).getString(rstr(keyResourceId), rstr(defaultValueResourceId))!!
    }

    fun getString(key: String, defaultValue: String, vararg pref: SharedPreferences): String {
        try {
            var finalValue = gp(*pref).getString(key, defaultValue)!!
            if (key == context.getString(com.jiadanni.launcher4d.R.string.pref_key__feed_weather_api_key) && finalValue != defaultValue) {
                finalValue = com.jiadanni.launcher4d.util.CryptoUtils.decrypt(finalValue)
            }
            return finalValue
        } catch (e: ClassCastException) {
            return defaultValue
        }
    }

    fun getString(
        @StringRes keyResourceId: Int,
        defaultValue: String,
        @StringRes keyResourceIdDefaultValue: Int,
        vararg pref: SharedPreferences
    ): String {
        return getString(rstr(keyResourceId), rstr(keyResourceIdDefaultValue), *pref)
    }

    private fun setStringListOne(key: String, values: List<String>, pref: SharedPreferences) {
        val sb = StringBuilder()
        for (value in values) {
            sb.append(ARRAY_SEPARATOR)
            sb.append(value.replace(ARRAY_SEPARATOR, ARRAY_SEPARATOR_SUBSTITUTE))
        }
        setString(key, sb.toString().replaceFirst(ARRAY_SEPARATOR.toRegex(), ""), pref)
    }

    private fun getStringListOne(key: String, pref: SharedPreferences): ArrayList<String> {
        val ret = ArrayList<String>()
        val value = getString(key, ARRAY_SEPARATOR, pref).replace(ARRAY_SEPARATOR_SUBSTITUTE, ARRAY_SEPARATOR)
        if (value == ARRAY_SEPARATOR || TextUtils.isEmpty(value)) {
            return ret
        }
        ret.addAll(listOf(*value.split(ARRAY_SEPARATOR.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()))
        return ret
    }

    fun setStringArray(@StringRes keyResourceId: Int, values: Array<String>, vararg pref: SharedPreferences) {
        setStringArray(rstr(keyResourceId), values, *pref)
    }

    fun setStringArray(key: String, values: Array<String>, vararg pref: SharedPreferences) {
        setStringListOne(key, values.asList(), gp(*pref))
    }

    fun setStringList(@StringRes keyResourceId: Int, values: List<String>, vararg pref: SharedPreferences) {
        setStringArray(rstr(keyResourceId), values.toTypedArray(), *pref)
    }

    fun setStringList(key: String, values: List<String>, vararg pref: SharedPreferences): SharedPreferencesPropertyBackend {
        setStringArray(key, values.toTypedArray(), *pref)
        return this
    }

    fun getStringArray(@StringRes keyResourceId: Int, vararg pref: SharedPreferences): Array<String> {
        return getStringArray(rstr(keyResourceId), *pref)
    }

    fun getStringArray(key: String, vararg pref: SharedPreferences): Array<String> {
        val list = getStringListOne(key, gp(*pref))
        return list.toTypedArray()
    }


    fun getStringList(@StringRes keyResourceId: Int, vararg pref: SharedPreferences): ArrayList<String> {
        return getStringListOne(rstr(keyResourceId), gp(*pref))
    }

    fun getStringList(key: String, vararg pref: SharedPreferences): ArrayList<String> {
        return getStringListOne(key, gp(*pref))
    }

    //
    // Getter & Setter for integer
    //
    fun setInt(@StringRes keyResourceId: Int, value: Int, vararg pref: SharedPreferences) {
        gp(*pref).edit().putInt(rstr(keyResourceId), value).apply()
    }

    fun setInt(key: String, value: Int, vararg pref: SharedPreferences): SharedPreferencesPropertyBackend {
        gp(*pref).edit().putInt(key, value).apply()
        return this
    }

    fun getInt(@StringRes keyResourceId: Int, defaultValue: Int, vararg pref: SharedPreferences): Int {
        return getInt(rstr(keyResourceId), defaultValue, *pref)
    }

    fun getInt(key: String, defaultValue: Int, vararg pref: SharedPreferences): Int {
        try {
            return gp(*pref).getInt(key, defaultValue)
        } catch (e: ClassCastException) {
            return defaultValue
        }
    }

    fun getIntOfStringPref(@StringRes keyResId: Int, defaultValue: Int, vararg pref: SharedPreferences): Int {
        return getIntOfStringPref(rstr(keyResId), defaultValue, gp(*pref))
    }

    fun getIntOfStringPref(key: String, defaultValue: Int, vararg pref: SharedPreferences): Int {
        val strNum = getString(key, Integer.toString(defaultValue), gp(*pref))
        return Integer.valueOf(strNum)
    }

    private fun setIntListOne(key: String, values: List<Int>, pref: SharedPreferences) {
        val sb = StringBuilder()
        for (value in values) {
            sb.append(ARRAY_SEPARATOR)
            sb.append(value.toString())
        }
        setString(key, sb.toString().replaceFirst(ARRAY_SEPARATOR.toRegex(), ""), pref)
    }

    private fun getIntListOne(key: String, pref: SharedPreferences): ArrayList<Int> {
        val ret = ArrayList<Int>()
        val value = getString(key, ARRAY_SEPARATOR, pref)
        if (value == ARRAY_SEPARATOR) {
            return ret
        }
        for (s in value.split(ARRAY_SEPARATOR.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            ret.add(Integer.parseInt(s))
        }
        return ret
    }

    fun setIntArray(@StringRes keyResourceId: Int, values: Array<Int>, vararg pref: SharedPreferences) {
        setIntArray(rstr(keyResourceId), values, gp(*pref))
    }

    fun setIntArray(key: String, values: Array<Int>, vararg pref: SharedPreferences) {
        setIntListOne(key, values.asList(), gp(*pref))
    }

    fun getIntArray(@StringRes keyResourceId: Int, vararg pref: SharedPreferences): Array<Int> {
        return getIntArray(rstr(keyResourceId), gp(*pref))
    }

    fun getIntArray(key: String, vararg pref: SharedPreferences): Array<Int> {
        val data = getIntListOne(key, gp(*pref))
        return data.toTypedArray()
    }


    fun setIntList(@StringRes keyResourceId: Int, values: List<Int>, vararg pref: SharedPreferences) {
        setIntListOne(rstr(keyResourceId), values, gp(*pref))
    }

    fun setIntList(key: String, values: List<Int>, vararg pref: SharedPreferences): SharedPreferencesPropertyBackend {
        setIntListOne(key, values, gp(*pref))
        return this
    }

    fun getIntList(@StringRes keyResourceId: Int, vararg pref: SharedPreferences): ArrayList<Int> {
        return getIntListOne(rstr(keyResourceId), gp(*pref))
    }

    fun getIntList(key: String, vararg pref: SharedPreferences): ArrayList<Int> {
        return getIntListOne(key, gp(*pref))
    }


    //
    // Getter & Setter for Long
    //
    fun setLong(@StringRes keyResourceId: Int, value: Long, vararg pref: SharedPreferences) {
        gp(*pref).edit().putLong(rstr(keyResourceId), value).apply()
    }

    fun setLong(key: String, value: Long, vararg pref: SharedPreferences): SharedPreferencesPropertyBackend {
        gp(*pref).edit().putLong(key, value).apply()
        return this
    }

    fun getLong(@StringRes keyResourceId: Int, defaultValue: Long, vararg pref: SharedPreferences): Long {
        return getLong(rstr(keyResourceId), defaultValue, *pref)
    }

    fun getLong(key: String, defaultValue: Long, vararg pref: SharedPreferences): Long {
        try {
            return gp(*pref).getLong(key, defaultValue)
        } catch (e: ClassCastException) {
            return defaultValue
        }
    }

    //
    // Getter & Setter for Float
    //
    fun setFloat(@StringRes keyResourceId: Int, value: Float, vararg pref: SharedPreferences) {
        gp(*pref).edit().putFloat(rstr(keyResourceId), value).apply()
    }

    fun setFloat(key: String, value: Float, vararg pref: SharedPreferences): SharedPreferencesPropertyBackend {
        gp(*pref).edit().putFloat(key, value).apply()
        return this
    }

    fun getFloat(@StringRes keyResourceId: Int, defaultValue: Float, vararg pref: SharedPreferences): Float {
        return getFloat(rstr(keyResourceId), defaultValue)
    }

    fun getFloat(key: String, defaultValue: Float, vararg pref: SharedPreferences): Float {
        try {
            return gp(*pref).getFloat(key, defaultValue)
        } catch (e: ClassCastException) {
            return defaultValue
        }
    }

    //
    // Getter & Setter for Double
    //
    fun setDouble(@StringRes keyResourceId: Int, value: Double, vararg pref: SharedPreferences) {
        setLong(rstr(keyResourceId), java.lang.Double.doubleToRawLongBits(value))
    }

    fun setDouble(key: String, value: Double, vararg pref: SharedPreferences): SharedPreferencesPropertyBackend {
        setLong(key, java.lang.Double.doubleToRawLongBits(value))
        return this
    }

    fun getDouble(@StringRes keyResourceId: Int, defaultValue: Double, vararg pref: SharedPreferences): Double {
        return getDouble(rstr(keyResourceId), defaultValue, gp(*pref))
    }

    fun getDouble(key: String, defaultValue: Double, vararg pref: SharedPreferences): Double {
        return java.lang.Double.longBitsToDouble(getLong(key, java.lang.Double.doubleToRawLongBits(defaultValue), gp(*pref)))
    }

    //
    // Getter & Setter for boolean
    //
    fun setBool(@StringRes keyResourceId: Int, value: Boolean, vararg pref: SharedPreferences) {
        gp(*pref).edit().putBoolean(rstr(keyResourceId), value).apply()
    }

    fun setBool(key: String, value: Boolean, vararg pref: SharedPreferences): SharedPreferencesPropertyBackend {
        gp(*pref).edit().putBoolean(key, value).apply()
        return this
    }

    fun getBool(@StringRes keyResourceId: Int, defaultValue: Boolean, vararg pref: SharedPreferences): Boolean {
        return getBool(rstr(keyResourceId), defaultValue)
    }

    fun getBool(key: String, defaultValue: Boolean, vararg pref: SharedPreferences): Boolean {
        try {
            return gp(*pref).getBoolean(key, defaultValue)
        } catch (e: ClassCastException) {
            return defaultValue
        }
    }

    //
    // Getter & Setter for Color
    //
    fun getColor(key: String, @ColorRes defaultColor: Int, vararg pref: SharedPreferences): Int {
        return getInt(key, rcolor(defaultColor))
    }

    fun getColor(@StringRes keyResourceId: Int, @ColorRes defaultColor: Int, vararg pref: SharedPreferences): Int {
        return getColor(rstr(keyResourceId), defaultColor)
    }

    //
    // PropertyBackend<String> implementations
    //
    override fun getString(key: String, defaultValue: String): String {
        return getString(key, defaultValue, _prefApp)
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return getInt(key, defaultValue, _prefApp)
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return getLong(key, defaultValue, _prefApp)
    }

    override fun getBool(key: String, defaultValue: Boolean): Boolean {
        return getBool(key, defaultValue, _prefApp)
    }

    override fun getFloat(key: String, defaultValue: Float): Float {
        return getFloat(key, defaultValue, _prefApp)
    }

    override fun getDouble(key: String, defaultValue: Double): Double {
        return getDouble(key, defaultValue, _prefApp)
    }

    override fun getIntList(key: String): ArrayList<Int> {
        return getIntList(key, _prefApp)
    }

    override fun getStringList(key: String): ArrayList<String> {
        return getStringList(key, _prefApp)
    }

    override fun setString(key: String, value: String): SharedPreferencesPropertyBackend {
        setString(key, value, _prefApp)
        return this
    }

    override fun setInt(key: String, value: Int): SharedPreferencesPropertyBackend {
        setInt(key, value, _prefApp)
        return this
    }

    override fun setLong(key: String, value: Long): SharedPreferencesPropertyBackend {
        setLong(key, value, _prefApp)
        return this
    }

    override fun setBool(key: String, value: Boolean): SharedPreferencesPropertyBackend {
        setBool(key, value, _prefApp)
        return this
    }

    override fun setFloat(key: String, value: Float): SharedPreferencesPropertyBackend {
        setFloat(key, value, _prefApp)
        return this
    }

    override fun setDouble(key: String, value: Double): SharedPreferencesPropertyBackend {
        setDouble(key, value, _prefApp)
        return this
    }

    override fun setIntList(key: String, value: List<Int>): SharedPreferencesPropertyBackend {
        setIntListOne(key, value, _prefApp)
        return this
    }

    override fun setStringList(key: String, value: List<String>): SharedPreferencesPropertyBackend {
        setStringListOne(key, value, _prefApp)
        return this
    }

    fun contains(key: String, vararg pref: SharedPreferences): Boolean {
        return gp(*pref).contains(key)
    }

    /**
     * Substract current datetime by given amount of days
     */
    fun getDateOfDaysAgo(days: Int): Date {
        val cal = GregorianCalendar()
        cal.add(Calendar.DATE, -days)
        return cal.time
    }

    /**
     * Substract current datetime by given amount of days and check if the given date passed
     */
    fun didDaysPassedSince(date: Date?, days: Int): Boolean {
        return date != null && days >= 0 && date.before(getDateOfDaysAgo(days))
    }

    fun afterDaysTrue(key: String, daysSinceLastTime: Int, firstTime: Int, vararg pref: SharedPreferences): Boolean {
        var d = Date(System.currentTimeMillis())
        if (!contains(key)) {
            d = getDateOfDaysAgo(daysSinceLastTime - firstTime)
            setLong(key, d.time)
            return firstTime < 1
        } else {
            d = Date(getLong(key, d.time))
        }
        val trigger = didDaysPassedSince(d, daysSinceLastTime)
        if (trigger) {
            setLong(key, Date(System.currentTimeMillis()).time)
        }
        return trigger
    }

    companion object {
        protected const val ARRAY_SEPARATOR = "%%%"
        protected const val ARRAY_SEPARATOR_SUBSTITUTE = "§§§"
        const val SHARED_PREF_APP = "app"
        private var _debugLog = ""


        fun limitListTo(list: MutableList<*>, maxSize: Int, removeDuplicates: Boolean) {
            var o: Any?
            var pos: Int

            if (removeDuplicates) {
                var i = 0
                while (i < list.size) {
                    o = list[i]
                    while (list.lastIndexOf(o).also { pos = it } != i && pos >= 0) {
                        list.removeAt(pos)
                    }
                    i++
                }
            }
            while (list.size.also { pos = it } > maxSize && pos > 0) {
                list.removeAt(list.size - 1)
            }
        }

        fun clearDebugLog() {
            _debugLog = ""
        }

        fun getDebugLog(): String {
            return _debugLog
        }

        @Synchronized
        fun appendDebugLog(text: String) {
            _debugLog += "[" + Date().toString() + "] " + text + "\n"
        }

        fun ne(str: String?): Boolean {
            return str != null && str.trim { it <= ' ' }.isNotEmpty()
        }

        fun fexists(fp: String?): Boolean {
            return ne(fp) && File(fp).exists()
        }

        /**
         * A method to determine if current hour is between begin and end.
         * This is especially useful for time-based light/dark mode
         */
        fun isCurrentHourOfDayBetween(begin: Int, end: Int): Boolean {
            var begin = begin
            var end = end
            begin = if (begin >= 23 || begin < 0) 0 else begin
            end = if (end >= 23 || end < 0) 0 else end
            val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return h >= begin && h <= end
        }
    }
}
