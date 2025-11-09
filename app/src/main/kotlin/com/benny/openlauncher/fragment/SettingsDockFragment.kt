package com.benny.openlauncher.fragment

import android.os.Bundle
import com.benny.openlauncher.R

class SettingsDockFragment : SettingsBaseFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        addPreferencesFromResource(R.xml.preferences_dock)
    }
}
