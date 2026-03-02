/*#######################################################
 *
 *   Maintained 2018-2023 by Gregor Santner <gsantner AT mailbox DOT org>
 *
 *   License: Apache 2.0
 *  https://github.com/gsantner/opoc/#licensing
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 #########################################################*/
/*
 * This class is not intended to be used directly.
 * Copy this file from opoc to your app and modify
 * packageId, resources and arguments to needs and availability
 */
package com.jiadanni.launcher4d.fragment

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.preference.Preference
import androidx.preference.PreferenceGroup
import com.jiadanni.launcher4d.R
import com.jiadanni.launcher4d.activity.SettingsActivity
import com.jiadanni.launcher4d.util.AppSettings
import net.gsantner.opoc.format.markdown.SimpleMarkdownParser
import net.gsantner.opoc.preference.GsPreferenceFragmentCompat
import net.gsantner.opoc.util.ActivityUtils
import net.gsantner.opoc.util.ShareUtil
import java.io.IOException
import java.util.Locale

class SettingsAboutFragment : GsPreferenceFragmentCompat<AppSettings>() {

    override fun getPreferenceResourceForInflation(): Int {
        return R.xml.preferences_about
    }

    override fun getFragmentTag(): String {
        return TAG
    }

    override fun getAppSettings(context: Context): AppSettings {
        return _appSettings ?: AppSettings(context)
    }

    override fun onPreferenceClicked(preference: Preference, key: String, keyResId: Int): Boolean? {
        val au = ActivityUtils(activity)
        if (isAdded && preference.hasKey()) {
            when (keyToStringResId(preference)) {
                R.string.pref_key__more_info__app -> {
                    _cu.openWebpageInExternalBrowser(getString(R.string.app_web_url))
                    return true
                }
                R.string.pref_key__more_info__settings -> {
                    au.animateToActivity(SettingsActivity::class.java, false, 124)
                    return true
                }
                R.string.pref_key__more_info__rate_app -> {
                    au.showGooglePlayEntryForThisApp()
                    return true
                }
                R.string.pref_key__more_info__join_community -> {
                    _cu.openWebpageInExternalBrowser(getString(R.string.app_community_url))
                    return true
                }
                R.string.pref_key__more_info__bug_reports -> {
                    _cu.openWebpageInExternalBrowser(getString(R.string.app_bug_report_url))
                    return true
                }
                R.string.pref_key__more_info__translate -> {
                    _cu.openWebpageInExternalBrowser(getString(R.string.app_translate_url))
                    return true
                }
                R.string.pref_key__more_info__project_contribution_info -> {
                    _cu.openWebpageInExternalBrowser(getString(R.string.app_contribution_info_url))
                    return true
                }
                R.string.pref_key__more_info__source_code -> {
                    _cu.openWebpageInExternalBrowser(getString(R.string.app_source_code_url))
                    return true
                }
                R.string.pref_key__more_info__project_license -> {
                    try {
                        val html = SimpleMarkdownParser().parse(
                            resources.openRawResource(R.raw.license),
                            "",
                            SimpleMarkdownParser.FILTER_ANDROID_TEXTVIEW
                        ).html
                        au.showDialogWithHtmlTextView(R.string.licenses, html)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                    return true
                }
                R.string.pref_key__more_info__open_source_licenses -> {
                    try {
                        val html = SimpleMarkdownParser().parse(
                            resources.openRawResource(R.raw.licenses),
                            "",
                            SimpleMarkdownParser.FILTER_ANDROID_TEXTVIEW
                        ).html
                        au.showDialogWithHtmlTextView(R.string.licenses, html)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                    return true
                }
                R.string.pref_key__more_info__contributors_public_info -> {
                    try {
                        val html = SimpleMarkdownParser().parse(
                            resources.openRawResource(R.raw.contributors),
                            "",
                            SimpleMarkdownParser.FILTER_ANDROID_TEXTVIEW
                        ).html
                        au.showDialogWithHtmlTextView(R.string.contributors, html)
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                    return true
                }
                R.string.pref_key__more_info__copy_build_information -> {
                    ShareUtil(context).setClipboard(preference.summary)
                    val smp = SimpleMarkdownParser()
                    try {
                        val html = smp.parse(
                            resources.openRawResource(R.raw.changelog),
                            "",
                            SimpleMarkdownParser.FILTER_ANDROID_TEXTVIEW,
                            SimpleMarkdownParser.FILTER_CHANGELOG
                        ).html
                        au.showDialogWithHtmlTextView(R.string.changelog, html)
                    } catch (ex: Exception) {
                        // Ignore
                    }
                    return true
                }
            }
        }
        return null
    }

    override fun isAllowedToTint(pref: Preference): Boolean {
        return getString(R.string.pref_key__more_info__app) != pref.key
    }

    override fun doUpdatePreferences() {
        super.doUpdatePreferences()
        val context = context ?: return
        val locale = Locale.getDefault()
        var tmp: String
        var pref: Preference?

        updateSummary(R.string.pref_key__more_info__project_license, getString(R.string.app_license_name))

        // Basic app info
        pref = findPreference(R.string.pref_key__more_info__app)
        if (pref != null && pref.summary == null) {
            pref.setIcon(R.mipmap.ic_launcher)
            pref.summary = String.format(
                locale,
                "%s\nVersion v%s (%d)",
                _cu.packageIdReal,
                _cu.appVersionName,
                _cu.bcint("VERSION_CODE", 0)
            )
        }

        // Extract some build information and publish in summary
        pref = findPreference(R.string.pref_key__more_info__copy_build_information)
        if (pref != null && pref.summary == null) {
            var summary = String.format(
                locale,
                "\n<b>Package:</b> %s\n<b>Version:</b> v%s (%d)",
                _cu.packageIdReal,
                _cu.appVersionName,
                _cu.bcint("VERSION_CODE", 0)
            )

            tmp = _cu.bcstr("FLAVOR", "")
            summary += if (tmp.isEmpty()) "" else "\n<b>Flavor:</b> ${tmp.replace("flavor", "")}"

            tmp = _cu.bcstr("BUILD_TYPE", "")
            summary += if (tmp.isEmpty()) "" else " ($tmp)"

            tmp = _cu.bcstr("BUILD_DATE", "")
            summary += if (tmp.isEmpty()) "" else "\n<b>Build date:</b> $tmp"

            tmp = _cu.appInstallationSource
            summary += if (tmp.isEmpty()) "" else "\n<b>ISource:</b> $tmp"

            tmp = _cu.bcstr("GITHASH", "")
            summary += if (tmp.isEmpty()) "" else "\n<b>VCS Hash:</b> $tmp"

            pref.summary = _cu.htmlToSpanned(summary.trim().replace("\n", "<br/>"))
        }

        // Extract project team from raw resource, where 1 person = 4 lines
        // 1) Name/Title, 2) Description/Summary, 3) Link/View-Intent, 4) Empty line
        pref = findPreference(R.string.pref_key__more_info__project_team)
        if (pref is PreferenceGroup && pref.preferenceCount == 0) {
            val data = (_cu.readTextfileFromRawRes(R.raw.project, "", "").trim() + "\n\n").split("\n")
            var i = 0
            while (i + 2 < data.size) {
                val person = Preference(context).apply {
                    title = data[i]
                    summary = data[i + 1]
                    setIcon(R.drawable.ic_person)
                    try {
                        val uri = Uri.parse(data[i + 2])
                        intent = Intent(Intent.ACTION_VIEW, uri).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    } catch (ignored: Exception) {
                        // Ignore
                    }
                }
                appendPreference(person, pref)
                i += 4
            }
        }
    }

    companion object {
        const val TAG = "MoreInfoFragment"

        @JvmStatic
        fun newInstance(): SettingsAboutFragment {
            return SettingsAboutFragment()
        }
    }
}
