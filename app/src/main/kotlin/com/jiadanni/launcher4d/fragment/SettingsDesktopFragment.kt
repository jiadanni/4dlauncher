package com.jiadanni.launcher4d.fragment

import android.os.Bundle
import androidx.preference.Preference
import com.jiadanni.launcher4d.R
import com.jiadanni.launcher4d.util.LauncherAction
import net.gsantner.opoc.util.ContextUtils

class SettingsDesktopFragment : SettingsBaseFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        addPreferencesFromResource(R.xml.preferences_desktop)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        val key = ContextUtils(requireContext()).getResId(ContextUtils.ResType.STRING, preference.key)
        return when (key) {
            R.string.pref_key__minibar -> {
                LauncherAction.runAction(LauncherAction.Action.EditMinibar, requireActivity())
                true
            }
            else -> false
        }
    }
}
