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
 * Parses most common markdown tags. Only inline tags are supported, multiline/block syntax
 * is not supported (citation, multiline code, ..). This is intended to stay as easy as possible.
 *
 * You can e.g. apply a accent color by replacing #000001 with your accentColor string.
 *
 * FILTER_ANDROID_TEXTVIEW output is intended to be used at simple Android TextViews,
 * were a limited set of _html tags is supported. This allow to still display e.g. a simple
 * CHANGELOG.md file without including a WebView for showing HTML, or other additional UI-libraries.
 *
 * FILTER_WEB is intended to be used at engines understanding most common HTML tags.
 */

package net.gsantner.opoc.format.markdown

import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Simple Markdown Parser
 */
@Suppress(
    "WeakerAccess",
    "CaughtExceptionImmediatelyRethrown",
    "SameParameterValue",
    "unused",
    "SpellCheckingInspection",
    "RepeatedSpace",
    "SingleCharAlternation",
    "Convert2Lambda"
)
class SimpleMarkdownParser {
    //########################
    //## Members, Constructors
    //########################
    private var _defaultSmpFilter: SmpFilter
    var html: String? = null
        private set

    init {
        _defaultSmpFilter = FILTER_WEB
    }

    //########################
    //## Methods
    //########################
    fun setDefaultSmpFilter(defaultSmpFilter: SmpFilter): SimpleMarkdownParser {
        _defaultSmpFilter = defaultSmpFilter
        return this
    }

    @Throws(IOException::class)
    fun parse(filepath: String, vararg smpFilters: SmpFilter): SimpleMarkdownParser {
        return parse(FileInputStream(filepath), "", *smpFilters)
    }

    @Throws(IOException::class)
    fun parse(inputStream: InputStream, lineMdPrefix: String, vararg smpFilters: SmpFilter): SimpleMarkdownParser {
        val sb = StringBuilder()
        var br: BufferedReader? = null
        var line: String?

        try {
            br = BufferedReader(InputStreamReader(inputStream))
            while (br.readLine().also { line = it } != null) {
                sb.append(lineMdPrefix)
                sb.append(line)
                sb.append("\n")
            }
        } catch (rethrow: IOException) {
            html = ""
            throw rethrow
        } finally {
            if (br != null) {
                try {
                    br.close()
                } catch (ignored: IOException) { }
            }
        }
        html = parse(sb.toString(), "", *smpFilters).html
        return this
    }

    @Throws(IOException::class)
    fun parse(markdown: String, lineMdPrefix: String, vararg smpFilters: SmpFilter): SimpleMarkdownParser {
        var smpFilters = smpFilters
        html = markdown
        if (smpFilters.isEmpty()) {
            smpFilters = arrayOf(_defaultSmpFilter)
        }
        for (smpFilter in smpFilters) {
            html = smpFilter.filter(html!!).trim { it <= ' ' }
        }
        return this
    }

    fun setHtml(html: String): SimpleMarkdownParser {
        this.html = html
        return this
    }

    fun removeMultiNewlines(): SimpleMarkdownParser {
        html = html!!.replace("\n", "").replace("(<br/>){3,}".toRegex(), "<br/><br/>")
        return this
    }

    fun replaceBulletCharacter(replacment: String): SimpleMarkdownParser {
        html = html!!.replace("&#8226;", replacment)
        return this
    }

    fun replaceColor(hexColor: String, newIntColor: Int): SimpleMarkdownParser {
        html = html!!.replace(hexColor, String.format("#%06X", 0xFFFFFF and newIntColor))
        return this
    }

    override fun toString(): String {
        return if (html != null) html!! else ""
    }

    //########################
    //## Statics
    //########################
    interface SmpFilter {
        fun filter(text: String): String
    }

    companion object {
        val FILTER_ANDROID_TEXTVIEW: SmpFilter = object : SmpFilter {
            override fun filter(text: String): String {
                // TextView supports a limited set of html tags, most notably
                // a href, b, big, font size&color, i, li, small, u

                // Don't start new line if 2 empty lines and heading
                var text = text
                while (text.contains("\n\n#")) {
                    text = text.replace("\n\n#", "\n#")
                }

                return text
                    .replace("(?s)<!--.*?-->".toRegex(), "")  // HTML comments
                    .replace("\n\n", "\n<br/>\n") // Start new line if 2 empty lines
                    .replace("~°", "&nbsp;&nbsp;") // double space/half tab
                    .replace("(?m)^### (.*)$".toRegex(), "<br/><big><b><font color='#000000'>$1</font></b></big><br/>") // h3
                    .replace(
                        "(?m)^## (.*)$".toRegex(),
                        "<br/><big><big><b><font color='#000000'>$1</font></b></big></big><br/><br/>"
                    ) // h2 (DEP: h3)
                    .replace(
                        "(?m)^# (.*)$".toRegex(),
                        "<br/><big><big><big><b><font color='#000000'>$1</font></b></big></big></big><br/><br/>"
                    ) // h1 (DEP: h2,h3)
                    .replace("!\\[(.*?)\\]\\((.*?)\\)".toRegex(), "<a href='$2'>$1</a>") // img
                    .replace("\\[(.*?)\\]\\((.*?)\\)".toRegex(), "<a href='$2'>$1</a>") // a href (DEP: img)
                    .replace("<(http|https):\\\\/\\\\/.(.*)>\\}".toRegex(), "<a href='$1://$2'>$1://$2</a>") // a href (DEP: img)
                    .replace(
                        "(?m)^([-*] )(.*)$".toRegex(),
                        "<font color='#000001'>&#8226;</font> $2<br/>"
                    ) // unordered list + end line
                    .replace(
                        "(?m)^  (-|\\*) ([^<]*)$".toRegex(),
                        "&nbsp;&nbsp;<font color='#000001'>&#8226;</font> $2<br/>"
                    ) // unordered list2 + end line
                    .replace("`([^<]*)`".toRegex(), "<font face='monospace'>$1</font>") // code
                    .replace("\\\\*", "●") // temporary replace escaped star symbol
                    .replace("(?m)\\\\*\\\\*(.*)\\\\*\\\\*".toRegex(), "<b>$1</b>") // bold (DEP: temp star)
                    .replace("(?m)\\\\*(.*)\\\\*".toRegex(), "<i>$1</i>") // italic (DEP: temp star code)
                    .replace("●", "*") // restore escaped star symbol (DEP: b,i)
                    .replace("(?m)  $".toRegex(), "<br/>") // new line (DEP: ul)
            }
        }

        val FILTER_WEB: SmpFilter = object : SmpFilter {
            override fun filter(text: String): String {
                // Don't start new line if 2 empty lines and heading
                var text = text
                while (text.contains("\n\n#")) {
                    text = text.replace("\n\n#", "\n#")
                }

                text = text
                    .replace("(?s)<!--.*?-->".toRegex(), "")  // HTML comments
                    .replace("\n\n", "\n<br/>\n") // Start new line if 2 empty lines
                    .replace("~°", "&nbsp;&nbsp;") // double space/half tab
                    .replace("(?m)^### (.*)$".toRegex(), "<h3>$1</h3>") // h3
                    .replace("(?m)^## (.*)$".toRegex(), "<h2>$1</h2>") /// h2 (DEP: h3)
                    .replace("(?m)^# (.*)$".toRegex(), "<h1>$1</h1>") // h1 (DEP: h2,h3)
                    .replace("!\\[(.*?)\\]\\((.*?)\\)".toRegex(), "<img src='$2' alt='$1' />") // img
                    .replace("<(http|https):\\\\/\\\\/.(.*)>\\}".toRegex(), "<a href='$1://$2'>$1://$2</a>") // a href (DEP: img)
                    .replace("\\[(.*?)\\]\\((.*?)\\)".toRegex(), "<a href='$2'>$1</a>") // a href (DEP: img)
                    .replace(
                        "(?m)^[-*] (.*)$".toRegex(),
                        "<font color='#000001'>&#8226;</font> $1  "
                    ) // unordered list + end line
                    .replace(
                        "(?m)^  [-*] (.*)$".toRegex(),
                        "&nbsp;&nbsp;<font color='#000001'>&#8226;</font> $1  "
                    ) // unordered list2 + end line
                    .replace("`([^<]*)`".toRegex(), "<code>$1</code>") // code
                    .replace("\\\\*", "●") // temporary replace escaped star symbol
                    .replace("(?m)\\\\*\\\\*(.*)\\\\*\\\\*".toRegex(), "<b>$1</b>") // bold (DEP: temp star)
                    .replace("(?m)\\\\*(.*)\\\\*".toRegex(), "<i>$1</i>") // italic (DEP: temp star code)
                    .replace("●", "*") // restore escaped star symbol (DEP: b,i)
                    .replace("(?m)  $".toRegex(), "<br/>") // new line (DEP: ul)
                return text
            }
        }

        val FILTER_CHANGELOG: SmpFilter = object : SmpFilter {
            override fun filter(text: String): String {
                var text = text
                text = text
                    .replace("New:", "<font color='#276230'>New:</font>")
                    .replace("New features:", "<font color='#276230'>New:</font>")
                    .replace("Added:", "<font color='#276230'>Added:</font>")
                    .replace("Add:", "<font color='#276230'>Add:</font>")
                    .replace("Fixed:", "<font color='#005688'>Fixed:</font>")
                    .replace("Fix:", "<font color='#005688'>Fix:</font>")
                    .replace("Removed:", "<font color='#C13524'>Removed:</font>")
                    .replace("Updated:", "<font color='#555555'>Updated:</font>")
                    .replace("Improved:", "<font color='#555555'>Improved:</font>")
                    .replace("Modified:", "<font color='#555555'>Modified:</font>")
                    .replace("Mod:", "<font color='#555555'>Mod:</font>")
                return text
            }
        }
        val FILTER_H_TO_SUP: SmpFilter = object : SmpFilter {
            override fun filter(text: String): String {
                var text = text
                text = text
                    .replace("<h1>", "<sup><sup><sup>")
                    .replace("</h1>", "</sup></sup></sup>")
                    .replace("<h2>", "<sup><sup>")
                    .replace("</h2>", "</sup></sup>")
                    .replace("<h3>", "<sup>")
                    .replace("</h3>", "</sup>")
                return text
            }
        }
        val FILTER_NONE: SmpFilter = object : SmpFilter {
            override fun filter(text: String): String {
                return text
            }
        }
    } // Closing brace for companion object

    //########################
    //## Singleton
    //########################
    private var __instance: SimpleMarkdownParser? = null

    fun get(): SimpleMarkdownParser {
        if (__instance == null) {
            __instance = SimpleMarkdownParser()
        }
        return __instance!!
    }
}
