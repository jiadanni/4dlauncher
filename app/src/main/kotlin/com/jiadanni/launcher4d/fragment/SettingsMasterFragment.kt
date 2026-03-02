package com.jiadanni.launcher4d.fragment

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import com.jiadanni.launcher4d.R
import com.jiadanni.launcher4d.activity.HideAppsActivity
import com.jiadanni.launcher4d.activity.MoreInfoActivity
import com.jiadanni.launcher4d.util.AppSettings
import com.jiadanni.launcher4d.widget.AppDrawerController
import net.gsantner.opoc.util.ContextUtils
import java.util.Locale

class SettingsMasterFragment : SettingsBaseFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        addPreferencesFromResource(R.xml.preferences_master)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        super.onPreferenceTreeClick(preference)
        val key = ContextUtils(requireContext()).getResId(ContextUtils.ResType.STRING, preference.key)

        return when (key) {
            R.string.pref_key__cat_hide_apps -> {
                val intent = Intent(activity, HideAppsActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                }
                startActivity(intent)
                true
            }
            R.string.pref_key__cat_about -> {
                startActivity(Intent(activity, MoreInfoActivity::class.java))
                true
            }
            else -> false
        }
    }

    override fun updateSummaries() {
        val categoryDesktop = findPreference<Preference>(getString(R.string.pref_key__cat_desktop))
        val categoryDock = findPreference<Preference>(getString(R.string.pref_key__cat_dock))
        val categoryAppDrawer = findPreference<Preference>(getString(R.string.pref_key__cat_app_drawer))
        val categoryAppearance = findPreference<Preference>(getString(R.string.pref_key__cat_appearance))

        categoryDesktop?.summary = String.format(
            Locale.ENGLISH,
            "%s: %d x %d",
            getString(R.string.pref_title__size),
            AppSettings.get().desktopColumnCount,
            AppSettings.get().desktopRowCount
        )

        categoryDock?.summary = String.format(
            Locale.ENGLISH,
            "%s: %d x %d",
            getString(R.string.pref_title__size),
            AppSettings.get().dockColumnCount,
            AppSettings.get().dockRowCount
        )

        categoryAppearance?.summary = String.format(
            Locale.ENGLISH,
            "%s: %ddp",
            getString(R.string.pref_title__icons),
            AppSettings.get().iconSize
        )

        categoryAppDrawer?.summary = when (AppSettings.get().drawerStyle) {
            AppDrawerController.Mode.GRID -> {
                String.format("%s: %s", getString(R.string.pref_title__style), getString(R.string.vertical_scroll_drawer))
            }
            AppDrawerController.Mode.PAGE -> {
                String.format("%s: %s", getString(R.string.pref_title__style), getString(R.string.horizontal_paged_drawer))
            }
            else -> {
                String.format("%s: %s", getString(R.string.pref_title__style), getString(R.string.horizontal_paged_drawer))
            }
        }
    }
}
