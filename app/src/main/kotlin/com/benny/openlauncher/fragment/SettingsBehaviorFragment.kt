package com.benny.openlauncher.fragment

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import com.benny.openlauncher.R
import com.benny.openlauncher.activity.HomeActivity
import com.benny.openlauncher.util.AppManager
import com.benny.openlauncher.util.AppSettings
import com.benny.openlauncher.util.LauncherAction
import com.benny.openlauncher.util.Tool
import com.benny.openlauncher.viewutil.DialogHelper
import net.gsantner.opoc.util.ContextUtils
import java.util.Locale

class SettingsBehaviorFragment : SettingsBaseFragment() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        super.onCreatePreferences(savedInstanceState, rootKey)
        addPreferencesFromResource(R.xml.preferences_behavior)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        val homeActivity = HomeActivity._launcher
        val key = ContextUtils(homeActivity).getResId(ContextUtils.ResType.STRING, preference.key)

        when (key) {
            R.string.pref_key__gesture_double_tap,
            R.string.pref_key__gesture_swipe_up,
            R.string.pref_key__gesture_swipe_down,
            R.string.pref_key__gesture_pinch_in,
            R.string.pref_key__gesture_pinch_out -> {
                DialogHelper.selectGestureDialog(requireActivity(), preference.title.toString()) { _, _, position, _ ->
                    when (position) {
                        1 -> {
                            DialogHelper.selectActionDialog(requireActivity()) { _, _, actionPosition, _ ->
                                AppSettings.get().setString(key, LauncherAction.getActionItem(actionPosition)._action.toString())
                            }
                        }
                        2 -> {
                            DialogHelper.selectAppDialog(requireActivity()) { app ->
                                AppSettings.get().setString(key, Tool.getIntentAsString(Tool.getIntentFromApp(app)))
                            }
                        }
                        else -> {
                            AppSettings.get().setString(key, "")
                        }
                    }
                }
            }
        }
        return false
    }

    override fun updateSummaries() {
        val gestures = listOf(
            R.string.pref_key__gesture_double_tap,
            R.string.pref_key__gesture_swipe_up,
            R.string.pref_key__gesture_swipe_down,
            R.string.pref_key__gesture_pinch_in,
            R.string.pref_key__gesture_pinch_out
        )

        for (resId in gestures) {
            val preference = findPreference<Preference>(getString(resId))
            val gesture = AppSettings.get().getGesture(resId)

            preference?.summary = when (gesture) {
                is Intent -> {
                    val app = AppManager.getInstance(requireContext()).findApp(gesture)
                    String.format(Locale.ENGLISH, "%s: %s", getString(R.string.app), app?._label ?: "")
                }
                is LauncherAction.ActionDisplayItem -> {
                    String.format(Locale.ENGLISH, "%s: %s", getString(R.string.action), gesture._label)
                }
                else -> {
                    String.format(Locale.ENGLISH, "%s", getString(R.string.none))
                }
            }
        }
    }
}
