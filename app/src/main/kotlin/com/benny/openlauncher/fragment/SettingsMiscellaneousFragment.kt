package com.benny.openlauncher.fragment

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.preference.Preference
import com.benny.openlauncher.R
import com.benny.openlauncher.activity.HomeActivity
import com.benny.openlauncher.util.AppSettings
import com.benny.openlauncher.util.Definitions
import com.benny.openlauncher.viewutil.DialogHelper
import com.nononsenseapps.filepicker.FilePickerActivity
import net.gsantner.opoc.util.ContextUtils
import net.gsantner.opoc.util.PermissionChecker

class SettingsMiscellaneousFragment : SettingsBaseFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        addPreferencesFromResource(R.xml.preferences_advanced)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        val homeActivity = HomeActivity._launcher
        val key = ContextUtils(homeActivity).getResId(ContextUtils.ResType.STRING, preference.key)

        return when (key) {
            R.string.pref_key__backup -> {
                if (PermissionChecker(requireActivity()).doIfExtStoragePermissionGranted()) {
                    val intent = Intent(activity, FilePickerActivity::class.java).apply {
                        putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true)
                        putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_DIR)
                    }
                    activity?.startActivityForResult(intent, Definitions.INTENT_BACKUP)
                }
                true
            }
            R.string.pref_key__restore -> {
                if (PermissionChecker(requireActivity()).doIfExtStoragePermissionGranted()) {
                    val intent = Intent(activity, FilePickerActivity::class.java).apply {
                        putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false)
                        putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE)
                    }
                    activity?.startActivityForResult(intent, Definitions.INTENT_RESTORE)
                }
                true
            }
            R.string.pref_key__reset_settings -> {
                DialogHelper.alertDialog(
                    requireActivity(),
                    getString(R.string.pref_title__reset_settings),
                    getString(R.string.are_you_sure)
                ) { _, _ ->
                    AppSettings.get().resetSettings()
                    homeActivity.recreate()
                    Toast.makeText(HomeActivity._launcher, R.string.toast_settings_restored, Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.string.pref_key__reset_database -> {
                DialogHelper.alertDialog(
                    requireActivity(),
                    getString(R.string.pref_title__reset_database),
                    getString(R.string.are_you_sure)
                ) { _, _ ->
                    val db = HomeActivity._db
                    db.onUpgrade(db.writableDatabase, 1, 1)
                    AppSettings.get().appFirstLaunch = true
                    homeActivity.recreate()
                    Toast.makeText(HomeActivity._launcher, R.string.toast_database_deleted, Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.string.pref_key__restart -> {
                homeActivity.recreate()
                activity?.finish()
                true
            }
            else -> false
        }
    }
}
