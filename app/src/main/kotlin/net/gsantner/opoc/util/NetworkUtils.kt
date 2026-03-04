/*#######################################################
 *
 *   Maintained 2017-2023 by Gregor Santner <gsantner AT mailbox DOT org>
 *
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *     https://github.com/gsantner/opoc/#licensing
 *
#########################################################*/
package net.gsantner.opoc.util

import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.UnsupportedEncodingException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.Charset
import java.util.HashMap

@Suppress("WeakerAccess", "unused", "SameParameterValue", "SpellCheckingInspection", "deprecation")
object NetworkUtils {
    private const val UTF8 = "UTF-8"
    const val GET = "GET"
    const val POST = "POST"
    const val PATCH = "PATCH"

    private const val BUFFER_SIZE = 4096

    // Downloads a file from the give url to the output file
    // Creates the file's parent directory if it doesn't exist
    fun downloadFile(url: String, out: File): Boolean {
        return downloadFile(url, out, null)
    }

    fun downloadFile(url: String, out: File, progressCallback: Callback.a1<Float>?): Boolean {
        return try {
            downloadFile(URL(url), out, null, progressCallback)
        } catch (e: MalformedURLException) {
            // Won't happen
            e.printStackTrace()
            false
        }
    }

    @JvmOverloads
    fun downloadFile(
        url: URL,
        outFile: File,
        connection: HttpURLConnection? = null,
        progressCallback: Callback.a1<Float>? = null
    ): Boolean {
        var connection = connection
        var input: InputStream? = null
        var output: OutputStream? = null
        try {
            if (connection == null) {
                connection = url.openConnection() as HttpURLConnection
            }
            connection.connect()
            input = if (connection.responseCode < HttpURLConnection.HTTP_BAD_REQUEST) connection.inputStream else connection.errorStream


            if (!outFile.parentFile.isDirectory)
                if (!outFile.parentFile.mkdirs())
                    return false
            output = FileOutputStream(outFile)

            var count: Int
            var written = 0
            val invLength = 1f / connection.contentLength

            val data = ByteArray(BUFFER_SIZE)
            while (input.read(data).also { count = it } != -1) {
                output.write(data, 0, count)
                if (invLength != -1f && progressCallback != null) {
                    written += count
                    progressCallback.callback(written * invLength)
                }
            }

            return true
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        } finally {
            try {
                output?.close()
                input?.close()
            } catch (ignored: IOException) {
            }
            connection?.disconnect()
        }
    }

    // No parameters, method can be GET, POST, etc.
    fun performCall(url: String, method: String): String {
        try {
            return performCall(URL(url), method, "")
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        }
        return ""
    }

    fun performCall(url: String, method: String, data: String): String {
        try {
            return performCall(URL(url), method, data)
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        }
        return ""
    }

    // URL encoded parameters
    fun performCall(url: String, method: String, params: HashMap<String, String>): String {
        try {
            return performCall(URL(url), method, encodeQuery(params))
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        }
        return ""
    }

    // Defaults to POST
    fun performCall(url: String, json: JSONObject): String {
        return performCall(url, POST, json)
    }

    fun performCall(url: String, method: String, json: JSONObject): String {
        try {
            return performCall(URL(url), method, json.toString())
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        }
        return ""
    }

    private fun performCall(url: URL, method: String, data: String): String {
        return performCall(url, method, data, null)
    }

    private fun performCall(url: URL, method: String, data: String?, existingConnection: HttpURLConnection?): String {
        try {
            val connection = existingConnection ?: url.openConnection() as HttpURLConnection
            connection.requestMethod = method
            connection.doInput = true

            if (data != null && !data.isEmpty()) {
                connection.doOutput = true
                val output = connection.outputStream
                output.write(data.toByteArray(Charset.forName("UTF-8")))
                output.flush()
                output.close()
            }

            val input =
                if (connection.responseCode < HttpURLConnection.HTTP_BAD_REQUEST) connection.inputStream else connection.errorStream

            return FileUtils.readCloseTextStream(connection.inputStream).joinToString("\n")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    @Throws(UnsupportedEncodingException::class)
    private fun encodeQuery(params: HashMap<String, String>): String {
        val result = StringBuilder()
        var first = true
        for ((key, value) in params) {
            if (first)
                first = false
            else
                result.append("&")

            result.append(URLEncoder.encode(key, UTF8))
            result.append("=")
            result.append(URLEncoder.encode(value, UTF8))
        }

        return result.toString()
    }

    fun getDataMap(query: String): HashMap<String, String> {
        val result = HashMap<String, String>()
        val sb = StringBuilder()
        var name = ""

        try {
            for (i in 0 until query.length) {
                val c = query[i]
                when (c) {
                    '=' -> {
                        name = URLDecoder.decode(sb.toString(), UTF8)
                        sb.setLength(0)
                    }
                    '&' -> {
                        result[name] = URLDecoder.decode(sb.toString(), UTF8)
                        sb.setLength(0)
                    }
                    else -> sb.append(c)
                }
            }
            if (name.isNotEmpty())
                result[name] = URLDecoder.decode(sb.toString(), UTF8)
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }

        return result
    }

    fun httpGetAsync(url: String, callback: Callback.a1<String>) {
        Thread {
            try {
                val c = performCall(url, GET)
                callback.callback(c)
            } catch (ignored: Exception) {
            }
        }.start()
    }
}
