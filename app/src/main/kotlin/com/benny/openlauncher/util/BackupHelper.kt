package com.benny.openlauncher.util

import android.content.Context
import android.widget.Toast
import com.benny.openlauncher.R
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupHelper {

    @JvmStatic
    fun backupConfig(context: Context, file: String) {
        val packageManager = context.packageManager
        try {
            val p = packageManager.getPackageInfo(context.packageName, 0)
            val dataDir = p.applicationInfo.dataDir

            FileOutputStream(file).use { fos ->
                BufferedOutputStream(fos).use { bos ->
                    ZipOutputStream(bos).use { zos ->
                        addFileToZip(zos, "$dataDir/databases/home.db", "home.db")
                        addFileToZip(zos, "$dataDir/shared_prefs/app.xml", "app.xml")
                        Toast.makeText(context, R.string.toast_backup_success, Toast.LENGTH_SHORT).show()
                        zos.flush()
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, R.string.toast_backup_error, Toast.LENGTH_SHORT).show()
        }
    }

    @JvmStatic
    fun restoreConfig(context: Context, file: String) {
        val packageManager = context.packageManager
        try {
            val p = packageManager.getPackageInfo(context.packageName, 0)
            val dataDir = p.applicationInfo.dataDir

            extractFileFromZip(file, "$dataDir/databases/home.db", "home.db")
            extractFileFromZip(file, "$dataDir/shared_prefs/app.xml", "app.xml")
            Toast.makeText(context, R.string.toast_backup_success, Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, R.string.toast_backup_error, Toast.LENGTH_SHORT).show()
        }
    }

    @JvmStatic
    fun addFileToZip(outZip: ZipOutputStream, file: String, name: String) {
        val data = ByteArray(Definitions.BUFFER_SIZE)
        FileInputStream(file).use { fi ->
            BufferedInputStream(fi, Definitions.BUFFER_SIZE).use { inputStream ->
                val entry = ZipEntry(name)
                outZip.putNextEntry(entry)
                var count: Int
                while (inputStream.read(data, 0, Definitions.BUFFER_SIZE).also { count = it } != -1) {
                    outZip.write(data, 0, count)
                }
            }
        }
    }

    @JvmStatic
    fun extractFileFromZip(filePath: String, file: String, name: String): Boolean {
        ZipInputStream(BufferedInputStream(FileInputStream(filePath))).use { inZip ->
            val data = ByteArray(Definitions.BUFFER_SIZE)
            var found = false

            var ze: ZipEntry?
            while (inZip.nextEntry.also { ze = it } != null) {
                if (ze?.name == name) {
                    found = true
                    // delete old file first
                    val oldFile = File(file)
                    if (oldFile.exists()) {
                        if (!oldFile.delete()) {
                            throw Exception("Could not delete $file")
                        }
                    }

                    FileOutputStream(file).use { outFile ->
                        var count: Int
                        while (inZip.read(data).also { count = it } != -1) {
                            outFile.write(data, 0, count)
                        }
                    }

                    inZip.closeEntry()
                }
            }
            return found
        }
    }
}
