package com.benny.openlauncher.fragment

import android.content.SharedPreferences
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.TypedValue
import androidx.appcompat.widget.Toolbar
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import com.benny.openlauncher.R
import com.benny.openlauncher.util.AppSettings

abstract class SettingsBaseFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = "app"
    }

    override fun onResume() {
        super.onResume()

        val toolbar = activity?.findViewById<Toolbar>(R.id.toolbar)
        toolbar?.title = preferenceScreen.title

        val sharedPreferences = AppSettings.get().defaultPreferences
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        updateIcons(preferenceScreen)
        updateSummaries()
    }

    override fun onPause() {
        super.onPause()

        val sharedPreferences = AppSettings.get().defaultPreferences
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        updateSummaries()
        if (key !in noRestart) {
            AppSettings.get().appRestartRequired = true
        }
    }

    open fun updateSummaries() {
        // override in fragments
    }

    fun updateIcons(prefGroup: PreferenceGroup?) {
        if (prefGroup != null && isAdded) {
            val prefCount = prefGroup.preferenceCount
            for (i in 0 until prefCount) {
                val preference = prefGroup.getPreference(i)
                if (preference != null) {
                    val drawable = preference.icon
                    if (drawable != null) {
                        val color = TypedValue()
                        context?.theme?.resolveAttribute(android.R.attr.textColorPrimary, color, true)
                        drawable.mutate().setColorFilter(resources.getColor(color.resourceId, null), PorterDuff.Mode.SRC_IN)
                    }

                    if (preference is PreferenceGroup) {
                        updateIcons(preference)
                    }
                }
            }
        }
    }

    companion object {
        private val noRestart = listOf(
            R.string.pref_key__gesture_double_tap,
            R.string.pref_key__gesture_swipe_up,
            R.string.pref_key__gesture_swipe_down,
            R.string.pref_key__gesture_pinch_in,
            R.string.pref_key__gesture_pinch_out
        )
    }
}
