package com.benny.openlauncher.fragment

import android.os.Bundle
import androidx.preference.Preference
import com.benny.openlauncher.R
import com.benny.openlauncher.activity.HomeActivity
import com.benny.openlauncher.util.LauncherAction
import net.gsantner.opoc.util.ContextUtils

class SettingsDesktopFragment : SettingsBaseFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        addPreferencesFromResource(R.xml.preferences_desktop)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        val homeActivity = HomeActivity._launcher
        val key = ContextUtils(homeActivity).getResId(ContextUtils.ResType.STRING, preference.key)
        return when (key) {
            R.string.pref_key__minibar -> {
                LauncherAction.runAction(LauncherAction.Action.EditMinibar, requireActivity())
                true
            }
            else -> false
        }
    }
}
