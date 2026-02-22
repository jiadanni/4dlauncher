/*#######################################################
 *
 *   Maintained 2018-2023 by Gregor Santner <gsantner AT mailbox DOT org>
 *
 *   License: Apache 2.0
 *  https://github.com/gsantner/opoc/#licensing
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
#########################################################*/
package net.gsantner.opoc.preference

@Suppress("UnusedReturnValue", "SpellCheckingInspection", "unused", "SameParameterValue")
interface PropertyBackend<TKEY, TTHIS> {
    fun getString(key: TKEY, defaultValue: String): String

    fun getInt(key: TKEY, defaultValue: Int): Int

    fun getLong(key: TKEY, defaultValue: Long): Long

    fun getBool(key: TKEY, defaultValue: Boolean): Boolean

    fun getFloat(key: TKEY, defaultValue: Float): Float

    fun getDouble(key: TKEY, defaultValue: Double): Double

    fun getIntList(key: TKEY): List<Int>

    fun getStringList(key: TKEY): List<String>

    fun setString(key: TKEY, value: String): TTHIS

    fun setInt(key: TKEY, value: Int): TTHIS

    fun setLong(key: TKEY, value: Long): TTHIS

    fun setBool(key: TKEY, value: Boolean): TTHIS

    fun setFloat(key: TKEY, value: Float): TTHIS

    fun setDouble(key: TKEY, value: Double): TTHIS

    fun setIntList(key: TKEY, value: List<Int>): TTHIS

    fun setStringList(key: TKEY, value: List<String>): TTHIS
}
