package com.jiadanni.launcher4d.fragment

import android.os.Bundle
import androidx.preference.Preference
import com.jiadanni.launcher4d.R
import com.jiadanni.launcher4d.viewutil.DialogHelper
import net.gsantner.opoc.util.ContextUtils

class SettingsAppearanceFragment : SettingsBaseFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        addPreferencesFromResource(R.xml.preferences_appearance)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        val key = ContextUtils(requireContext()).getResId(ContextUtils.ResType.STRING, preference.key)
        return when (key) {
            R.string.pref_key__icon_pack -> {
                DialogHelper.startPickIconPackIntent(requireActivity())
                true
            }
            else -> false
        }
    }
}
