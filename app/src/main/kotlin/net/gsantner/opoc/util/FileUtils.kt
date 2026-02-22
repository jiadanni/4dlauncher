/*#######################################################
 *
 *   Maintained 2017-2023 by Gregor Santner <gsantner @ mailbox . org>
 *
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *     https://github.com/gsantner/opoc/#licensing
 *
#########################################################*/
package net.gsantner.opoc.util


import android.text.TextUtils
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.URLConnection
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern

@Suppress("WeakerAccess", "unused", "SameParameterValue", "SpellCheckingInspection", "deprecation", "TryFinallyCanBeTryWithResources")
object FileUtils {
    // Used on methods like copyFile(src, dst)
    private const val BUFFER_SIZE = 4096

    fun readTextFileFast(file: File): String {
        try {
            return String(readCloseStreamWithSize(FileInputStream(file), file.length().toInt()))
        } catch (e: FileNotFoundException) {
            System.err.println("readTextFileFast: File $file not found.")
        }
        return ""
    }

    fun readCloseStreamWithSize(stream: InputStream, size: Int): ByteArray {
        val data = ByteArray(size)
        try {
            DataInputStream(stream).use { dis -> dis.readFully(data) }
        } catch (ignored: IOException) {
        }
        return data
    }

    fun readTextFile(file: File): String {
        try {
            return readCloseTextStream(FileInputStream(file))
        } catch (e: FileNotFoundException) {
            System.err.println("readTextFile: File $file not found.")
        }

        return ""
    }

    @JvmOverloads
    fun readCloseTextStream(stream: InputStream, concatToOneString: Boolean = true): List<String> {
        val lines = ArrayList<String>()
        var reader: BufferedReader? = null
        var line = ""
        try {
            val sb = StringBuilder()
            reader = BufferedReader(InputStreamReader(stream))

            while (reader.readLine().also { line = it } != null) {
                if (concatToOneString) {
                    sb.append(line).append('\n')
                } else {
                    lines.add(line)
                }
            }
            line = sb.toString()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (reader != null) {
                try {
                    reader.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        if (concatToOneString) {
            lines.clear()
            lines.add(line)
        }
        return lines
    }

    fun readBinaryFile(file: File): ByteArray {
        try {
            return readCloseBinaryStream(FileInputStream(file), file.length().toInt())
        } catch (e: FileNotFoundException) {
            System.err.println("readBinaryFile: File $file not found.")
        }

        return ByteArray(0)
    }

    fun readCloseBinaryStream(stream: InputStream, byteCount: Int): ByteArray {
        var reader: BufferedInputStream? = null
        val buf = ByteArray(byteCount)
        var totalBytesRead = 0
        try {
            reader = BufferedInputStream(stream)
            while (totalBytesRead < byteCount) {
                val bytesRead = reader.read(buf, totalBytesRead, byteCount - totalBytesRead)
                if (bytesRead > 0) {
                    totalBytesRead += bytesRead
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            if (reader != null) {
                try {
                    reader.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return buf
    }

    // Read binary stream (of unknown conf size)
    fun readCloseBinaryStream(stream: InputStream): ByteArray {
        val baos = ByteArrayOutputStream()
        try {
            val buffer = ByteArray(BUFFER_SIZE)
            var read: Int
            while (stream.read(buffer).also { read = it } != -1) {
                baos.write(buffer, 0, read)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                stream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return baos.toByteArray()
    }

    fun writeFile(file: File, data: ByteArray): Boolean {
        try {
            var output: OutputStream? = null
            try {
                output = BufferedOutputStream(FileOutputStream(file))
                output.write(data)
                output.flush()
                return true
            } finally {
                output?.close()
            }
        } catch (ex: Exception) {
            return false
        }
    }

    fun writeFile(file: File, content: String): Boolean {
        var writer: BufferedWriter? = null
        try {
            if (!file.parentFile.isDirectory && !file.parentFile.mkdirs())
                return false

            writer = BufferedWriter(FileWriter(file))
            writer.write(content)
            writer.flush()
            return true
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        } finally {
            if (writer != null) {
                try {
                    writer.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun copyFile(src: File, dst: File): Boolean {
        // Just touch file if src is empty
        if (src.length() == 0L) {
            return touch(dst)
        }

        var `is`: InputStream? = null
        var os: FileOutputStream? = null
        try {
            try {
                `is` = FileInputStream(src)
                os = FileOutputStream(dst)
                val buf = ByteArray(BUFFER_SIZE)
                var len: Int
                while (`is`.read(buf).also { len = it } > 0) {
                    os.write(buf, 0, len)
                }
                return true
            } finally {
                `is`?.close()
                os?.close()
            }
        } catch (ex: IOException) {
            return false
        }
    }

    fun copyFile(src: File, os: FileOutputStream): Boolean {
        var `is`: InputStream? = null
        try {
            try {
                `is` = FileInputStream(src)
                val buf = ByteArray(BUFFER_SIZE)
                var len: Int
                while (`is`.read(buf).also { len = it } > 0) {
                    os.write(buf, 0, len)
                }
                return true
            } finally {
                `is`?.close()
                os.close()
            }
        } catch (ex: IOException) {
            return false
        }
    }

    // Returns -1 if the file did not contain any of the needles, otherwise,
    // the index of which needle was found in the contents of the file.
    //
    // Needless MUST be in lower-case.
    fun fileContains(file: File, vararg needles: String): Int {
        try {
            val `in` = FileInputStream(file)

            var i: Int
            var line: String
            val reader = BufferedReader(InputStreamReader(`in`))
            while (reader.readLine().also { line = it } != null) {
                i = 0
                while (i != needles.size) {
                    if (line.toLowerCase(Locale.ROOT).contains(needles[i])) {
                        return i
                    }
                    ++i
                }
            }

            `in`.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return -1
    }

    fun deleteRecursive(file: File): Boolean {
        var ok = true
        if (file.exists()) {
            if (file.isDirectory) {
                for (child in file.listFiles())
                    ok = ok && deleteRecursive(child)
            }
            ok = ok && file.delete()
        }
        return ok
    }

    // Example: Check if this is maybe a conf: (str, "jpg", "png", "jpeg")
    fun hasExtension(str: String, vararg extensions: String): Boolean {
        val lc = str.toLowerCase(Locale.ROOT)
        for (extension in extensions) {
            if (lc.endsWith("." + extension.toLowerCase(Locale.ROOT))) {
                return true
            }
        }
        return false
    }

    fun renameFile(srcFile: File, destFile: File): Boolean {
        var srcFile = srcFile
        if (srcFile.absolutePath == destFile.absolutePath) {
            return false
        }

        // renameTo will fail in case of case-changed filename in same dir.Even on case-sensitive FS!!!
        if (srcFile.parent == destFile.parent && srcFile.name.toLowerCase(Locale.getDefault()) == destFile.name.toLowerCase(
                Locale.getDefault()
            )
        ) {
            val tmpFile = File(destFile.parent, UUID.randomUUID().leastSignificantBits.toString() + ".tmp")
            if (!tmpFile.exists()) {
                renameFile(srcFile, tmpFile)
                srcFile = tmpFile
            }
        }

        if (!srcFile.renameTo(destFile)) {
            if (copyFile(srcFile, destFile) && !srcFile.delete()) {
                return !destFile.delete()
            }
        }
        return true
    }

    fun renameFileInSameFolder(srcFile: File, destFilename: String): Boolean {
        return renameFile(srcFile, File(srcFile.parent, destFilename))
    }

    fun touch(file: File): Boolean {
        try {
            if (!file.exists()) {
                FileOutputStream(file).close()
            }
            return file.setLastModified(System.currentTimeMillis())
        } catch (e: IOException) {
            return false
        }
    }

    // Get relative path to specified destination
    fun relativePath(src: File, dest: File): String? {
        try {
            val srcSplit = (if (src.isDirectory) src else src.parentFile).canonicalPath.split(Pattern.quote(File.separator))
            val destSplit = dest.canonicalPath.split(Pattern.quote(File.separator))
            val sb = StringBuilder()
            var i = 0

            while (i < destSplit.size && i < srcSplit.size) {
                if (destSplit[i] != srcSplit[i])
                    break
                i++
            }
            if (i != srcSplit.size) {
                for (iUpperDir in i until srcSplit.size) {
                    sb.append("..")
                    sb.append(File.separator)
                }
            }
            while (i < destSplit.size) {
                sb.append(destSplit[i])
                sb.append(File.separator)
                i++
            }
            if (!dest.path.endsWith("/") && !dest.path.endsWith("\\")) {
                sb.delete(sb.length - File.separator.length, sb.length)
            }
            return sb.toString()
        } catch (exception: IOException) {
            return null
        } catch (exception: NullPointerException) {
            return null
        }
    }

    /**
     * Try to detect MimeType by backwards compatible methods
     */
    fun getMimeType(file: File?): String {
        var guess: String? = null
        if (file != null) {
            if (file.exists() && file.isFile) {
                var `is`: InputStream? = null
                try {
                    `is` = BufferedInputStream(FileInputStream(file))
                    guess = URLConnection.guessContentTypeFromStream(`is`)
                } catch (ignored: Exception) {
                } finally {
                    if (`is` != null) {
                        try {
                            `is`.close()
                        } catch (ignored: Exception) {
                        }
                    }
                }
            }

            val filename = file.name.replace(".jenc", "")
            val dot = filename.lastIndexOf(".") + 1
            if (dot > 0 && dot < filename.length) {
                when (filename.substring(dot)) {
                    "md", "markdown", "mkd", "mdown", "mkdn", "mdwn", "rmd" -> guess = "text/markdown"
                    "txt" -> guess = "text/plain"
                    "webp" -> guess = "image/webp"
                    "jpg", "jpeg" -> guess = "image/jpeg"
                    "png" -> guess = "image/png"
                }
            }

            if (TextUtils.isEmpty(guess)) {
                guess = URLConnection.guessContentTypeFromName(filename)
            }
        }

        return if (TextUtils.isEmpty(guess)) "*/*" else guess!!
    }

    fun isTextFile(file: File): Boolean {
        val mime = getMimeType(file)
        return mime != null && mime.startsWith("text/")
    }

    /**
     * Analyze given textfile and retrieve multiple information from it
     * Information is written back to the [AtomicInteger] parameters
     */
    fun retrieveTextFileSummary(
        file: File,
        numCharacters: AtomicInteger,
        numLines: AtomicInteger,
        numWords: AtomicInteger
    ) {
        var br: BufferedReader? = null
        try {
            br = BufferedReader(FileReader(file))
            var line: String?
            while (br.readLine().also { line = it } != null) {
                numLines.getAndIncrement()
                numCharacters.getAndSet(numCharacters.get() + line!!.length)
                if (line != "") {
                    numWords.getAndSet(numWords.get() + line!!.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray().size)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            numCharacters.set(-1)
            numLines.set(-1)
            numWords.set(-1)
        } finally {
            if (br != null) {
                try {
                    br.close()
                } catch (ignored: IOException) {
                }
            }
        }
    }

    /**
     * Format filesize to human readable format
     * Get size in bytes e.g. from [File] using `File#length()`
     */
    fun getReadableFileSize(size: Long, abbreviation: Boolean): String {
        if (size <= 0) {
            return "0B"
        }
        val units =
            if (abbreviation) arrayOf("B", "kB", "MB", "GB", "TB") else arrayOf(
                "Bytes",
                "Kilobytes",
                "Megabytes",
                "Gigabytes",
                "Terabytes"
            )
        val unit = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return DecimalFormat(
            "#,##0.#",
            DecimalFormatSymbols.getInstance(Locale.ENGLISH)
        ).format(size / Math.pow(1024.0, unit.toDouble())) + " " + units[unit]
    }

    fun getTimeDiffHMS(now: Long, past: Long): IntArray {
        val ret = IntArray(3)
        val diff = Math.abs(now - past)
        ret[0] = (diff / (1000 * 60 * 60)).toInt() // hours
        ret[1] = (diff / (1000 * 60) % 60).toInt() // min
        ret[2] = (diff / 1000 % 60).toInt() // sec
        return ret
    }

    fun getHumanReadableByteCountSI(bytes: Long): String {
        return if (bytes < 1000) {
            String.format(Locale.getDefault(), "%d%s", bytes, "B")
        } else if (bytes < 1000000) {
            String.format(Locale.getDefault(), "%.2f%s", bytes / 1000f, "KB")
        } else if (bytes < 1000000000) {
            String.format(Locale.getDefault(), "%.2f%s", bytes / 1000000f, "MB")
        } else if (bytes < 1000000000000L) {
            String.format(Locale.getDefault(), "%.2f%s", bytes / 1000000000f, "GB")
        } else {
            String.format(Locale.getDefault(), "%.2f%s", bytes / 1000000000000f, "TB")
        }
    }

    fun join(file: File, vararg childSegments: String?): File {
        var file = file
        for (s in childSegments ?: arrayOfNulls<String>(0)) {
            file = File(file, s)
        }
        return file
    }
}
