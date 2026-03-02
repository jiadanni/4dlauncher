package com.jiadanni.launcher4d.fragment

import android.os.Bundle
import com.jiadanni.launcher4d.R

class SettingsAppDrawerFragment : SettingsBaseFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        addPreferencesFromResource(R.xml.preferences_app_drawer)
    }
}
