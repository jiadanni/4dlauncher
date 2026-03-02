package com.jiadanni.launcher4d.fragment

import android.os.Bundle
import androidx.preference.EditTextPreference
import com.jiadanni.launcher4d.R
import com.jiadanni.launcher4d.util.AppSettings

/**
 * Settings fragment for feed configuration
 */
class SettingsFeedFragment : SettingsBaseFragment() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        addPreferencesFromResource(R.xml.preferences_feed)
    }

    override fun updateSummaries() {
        // Update weather API key summary to show masked value or "Not set"
        val weatherApiKeyPref = findPreference<EditTextPreference>(getString(R.string.pref_key__feed_weather_api_key))
        val apiKey = AppSettings.get().feedWeatherApiKey

        weatherApiKeyPref?.summary = when {
            apiKey.isBlank() -> "Not set - Get your free key at openweathermap.org"
            apiKey.length > 8 -> "${apiKey.substring(0, 4)}...${apiKey.substring(apiKey.length - 4)}"
            else -> "****"
        }
    }
}
