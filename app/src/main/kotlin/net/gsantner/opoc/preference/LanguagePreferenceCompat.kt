/*#######################################################
 * 
 *   Maintained 2018-2023 by Gregor Santner <gsantner @ mailbox . org>
 *
 *   License: Apache 2.0
 *  https://github.com/gsantner/opoc/#licensing
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/

/*
 * Requires:
      The BuildConfig field "APPLICATION_LANGUAGES" which is a array of all available languages
      opoc/ContextUtils
 * BuildConfig field can be defined by using the method below

buildConfigField "String[]", "APPLICATION_LANGUAGES", "${getUsedAndroidLanguages()}"

 @SuppressWarnings(["UnnecessaryQualifiedReference", "SpellCheckingInspection", "GroovyUnusedDeclaration"])
// Returns used android languages as a buildConfig array: {'de', 'it', ..}
static String getUsedAndroidLanguages() {
    Set<String> langs = new HashSet<>()
    new File('.').eachFileRecurse(groovy.io.FileType.DIRECTORIES) {
        final foldername = it.name
        if (foldername.startsWith('values-') && !it.canonicalPath.contains("build" + File.separator + "intermediates")) {
            new File(it.toString()).eachFileRecurse(groovy.io.FileType.FILES) {
                if (it.name.toLowerCase().endsWith(".xml") && it.getCanonicalFile().getText('UTF-8').contains("<string")) {
                    langs.add(foldername.replace("values-", ""))
                }
            }
        }
    }
    return '{' + langs.collect { "${it}" }.join(",") + '}'
}

 * Summary: Change language of this app. Restart app for changes to take effect

 * Define element in Preferences-XML:
    <net.gsantner.opoc.preference.LanguagePreferenceCompat
        android:icon="@drawable/ic_language_black_24dp"
        android:key="@string/pref_key__language"
        android:summary="@string/pref_desc__language"
        android:title="@string/pref_title__language"/>
 */
package net.gsantner.opoc.preference

import android.annotation.TargetApi
import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.text.TextUtils
import android.util.AttributeSet
import androidx.core.os.ConfigurationCompat
import androidx.preference.ListPreference
import net.gsantner.opoc.util.ContextUtils
import java.util.Collections
import java.util.Locale

/**
 * A [android.preference.ListPreference] that displays a list of languages to select from
 */
@Suppress("unused", "SpellCheckingInspection", "WeakerAccess")
open class LanguagePreferenceCompat : ListPreference {
    // The language of res/values/ -> (usually English)
    var systemLanguageName = "System"
    var defaultLanguageCode = "en"

    constructor(context: Context) : super(context) {
        loadLangs(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        loadLangs(context, attrs)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        loadLangs(context, attrs)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes
    ) {
        loadLangs(context, attrs)
    }

    override fun callChangeListener(newValue: Any?): Boolean {
        if (newValue is String) {
            // Does not apply to existing UI, use recreate()
            ContextUtils(context).setAppLanguage(newValue)
        }
        return super.callChangeListener(newValue)
    }


    private fun loadLangs(context: Context, attrs: AttributeSet?) {
        setDefaultValue(SYSTEM_LANGUAGE_CODE)

        // Fetch readable details
        val contextUtils = ContextUtils(context)
        val languages = ArrayList<String>()
        val bcof = contextUtils.getBuildConfigValue("DETECTED_ANDROID_LOCALES")
        if (bcof is Array<*>) {
            for (langId in bcof) {
                val locale = contextUtils.getLocaleByAndroidCode(langId.toString())
                languages.add(summarizeLocale(locale, langId.toString()) + ";" + langId)
            }
        }

        // Sort languages naturally
        Collections.sort(languages)

        // Show in UI
        val entries = arrayOfNulls<String>(languages.size + 2)
        val entryval = arrayOfNulls<String>(languages.size + 2)
        for (i in languages.indices) {
            entries[i + 2] = languages[i].split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
            entryval[i + 2] = languages[i].split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
        }
        entryval[0] = SYSTEM_LANGUAGE_CODE
        entries[0] =
            systemLanguageName + " » " + summarizeLocale(
                ConfigurationCompat.getLocales(Resources.getSystem().configuration)[0]!!,
                ""
            )
        entryval[1] = defaultLanguageCode
        entries[1] = summarizeLocale(contextUtils.getLocaleByAndroidCode(defaultLanguageCode), defaultLanguageCode)

        setEntries(entries)
        setEntryValues(entryval)
    }

    // Concat english and localized language name
    // Append country if country specific (e.g. Portuguese Brazil)
    private fun summarizeLocale(locale: Locale, localeAndroidCode: String): String {
        val country = locale.getDisplayCountry(locale)
        val language = locale.getDisplayLanguage(locale)
        var ret = (locale.getDisplayLanguage(Locale.ENGLISH)
                + " (" + language.substring(0, 1).toUpperCase(Locale.getDefault()) + language.substring(1)
                + (if (!country.isEmpty() && country.toLowerCase(Locale.getDefault()) != language.toLowerCase(Locale.getDefault())) ", $country" else "")
                + ")")

        if (localeAndroidCode == "zh-rCN") {
            ret = ret.substring(0, ret.indexOf(" ") + 1) + "Simplified" + ret.substring(ret.indexOf(" "))
        } else if (localeAndroidCode == "zh-rTW") {
            ret = ret.substring(0, ret.indexOf(" ") + 1) + "Traditional" + ret.substring(ret.indexOf(" "))
        } else if (localeAndroidCode == "sr-rRS") {
            ret = ret.substring(0, ret.indexOf(" ") + 1) + "Latin" + ret.substring(ret.indexOf(" "))
        } else if (localeAndroidCode.startsWith("sr")) {
            ret = ret.substring(0, ret.indexOf(" ") + 1) + "Cyrillic" + ret.substring(ret.indexOf(" "))
        } else if (localeAndroidCode == "fil") {
            ret = ret.substring(0, ret.indexOf("(") + 1) + "Philippines)"
        }

        return ret
    }

    // Add current language to summary
    override fun getSummary(): CharSequence {
        val locale = ContextUtils(context).getLocaleByAndroidCode(value)
        val prefix = if (TextUtils.isEmpty(super.getSummary()))
            ""
        else
            super.getSummary().toString() + "\n\n"
        return prefix + summarizeLocale(locale, value)
    }

    companion object {
        private const val SYSTEM_LANGUAGE_CODE = ""
    }
}
