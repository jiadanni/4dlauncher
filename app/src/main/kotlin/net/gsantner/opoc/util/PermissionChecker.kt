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

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

@Suppress("unused", "MemberVisibilityCanBePrivate")
class PermissionChecker(protected val activity: Activity) {

    fun doIfExtStoragePermissionGranted(vararg optionalToastMessageForKnowingWhyNeeded: String?): Boolean {
        // Android 13+ uses READ_MEDIA_IMAGES instead of READ_EXTERNAL_STORAGE
        val (permission, requestCode) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES to CODE_PERMISSION_READ_MEDIA_IMAGES
        } else {
            Manifest.permission.WRITE_EXTERNAL_STORAGE to CODE_PERMISSION_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
            if (optionalToastMessageForKnowingWhyNeeded.isNotEmpty() && optionalToastMessageForKnowingWhyNeeded[0] != null) {
                AlertDialog.Builder(activity)
                    .setMessage(optionalToastMessageForKnowingWhyNeeded[0])
                    .setCancelable(false)
                    .setNegativeButton(android.R.string.no, null)
                    .setPositiveButton(android.R.string.yes) { _, _ ->
                        if (Build.VERSION.SDK_INT >= 23) {
                            ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode)
                        }
                    }
                    .show()
                return false
            }
            ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode)
            return false
        }
        return true
    }

    fun doIfNotificationPermissionGranted(vararg optionalMessageForKnowingWhyNeeded: String?): Boolean {
        // Notification permission is only required on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {

                if (optionalMessageForKnowingWhyNeeded.isNotEmpty() && optionalMessageForKnowingWhyNeeded[0] != null) {
                    AlertDialog.Builder(activity)
                        .setMessage(optionalMessageForKnowingWhyNeeded[0])
                        .setCancelable(false)
                        .setNegativeButton(android.R.string.no, null)
                        .setPositiveButton(android.R.string.yes) { _, _ ->
                            ActivityCompat.requestPermissions(
                                activity,
                                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                                CODE_PERMISSION_POST_NOTIFICATIONS
                            )
                        }
                        .show()
                    return false
                }
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    CODE_PERMISSION_POST_NOTIFICATIONS
                )
                return false
            }
        }
        return true
    }

    fun checkPermissionResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray): Boolean {
        return if (grantResults.isNotEmpty()) {
            when (requestCode) {
                CODE_PERMISSION_EXTERNAL_STORAGE,
                CODE_PERMISSION_READ_MEDIA_IMAGES,
                CODE_PERMISSION_POST_NOTIFICATIONS -> grantResults[0] == PackageManager.PERMISSION_GRANTED
                else -> false
            }
        } else {
            false
        }
    }

    fun mkdirIfStoragePermissionGranted(dir: File): Boolean {
        return doIfExtStoragePermissionGranted() && (dir.exists() || dir.mkdirs())
    }

    companion object {
        const val CODE_PERMISSION_EXTERNAL_STORAGE = 4000
        const val CODE_PERMISSION_POST_NOTIFICATIONS = 4001
        const val CODE_PERMISSION_READ_MEDIA_IMAGES = 4002
    }
}
