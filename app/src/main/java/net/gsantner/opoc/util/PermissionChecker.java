/*#######################################################
 *
 *   Maintained 2017-2023 by Gregor Santner <gsantner AT mailbox DOT org>
 *
 *   License of this file: Apache 2.0
 *     https://www.apache.org/licenses/LICENSE-2.0
 *     https://github.com/gsantner/opoc/#licensing
 *
#########################################################*/
package net.gsantner.opoc.util;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;

import java.io.File;

@SuppressWarnings({"unused", "WeakerAccess"})
public class PermissionChecker {
    protected static final int CODE_PERMISSION_EXTERNAL_STORAGE = 4000;
    protected static final int CODE_PERMISSION_POST_NOTIFICATIONS = 4001;
    protected static final int CODE_PERMISSION_READ_MEDIA_IMAGES = 4002;

    protected Activity _activity;

    public PermissionChecker(Activity activity) {
        _activity = activity;
    }

    public boolean doIfExtStoragePermissionGranted(String... optionalToastMessageForKnowingWhyNeeded) {
        // Android 13+ uses READ_MEDIA_IMAGES instead of READ_EXTERNAL_STORAGE
        String permission;
        int requestCode;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
            requestCode = CODE_PERMISSION_READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
            requestCode = CODE_PERMISSION_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(_activity, permission) != PackageManager.PERMISSION_GRANTED) {
            if (optionalToastMessageForKnowingWhyNeeded != null && optionalToastMessageForKnowingWhyNeeded.length > 0 && optionalToastMessageForKnowingWhyNeeded[0] != null) {
                new AlertDialog.Builder(_activity)
                        .setMessage(optionalToastMessageForKnowingWhyNeeded[0])
                        .setCancelable(false)
                        .setNegativeButton(android.R.string.no, null)
                        .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                            if (Build.VERSION.SDK_INT >= 23) {
                                ActivityCompat.requestPermissions(_activity, new String[]{permission}, requestCode);
                            }
                        })
                        .show();
                return false;
            }
            ActivityCompat.requestPermissions(_activity, new String[]{permission}, requestCode);
            return false;
        }
        return true;
    }

    public boolean doIfNotificationPermissionGranted(String... optionalMessageForKnowingWhyNeeded) {
        // Notification permission is only required on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(_activity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                if (optionalMessageForKnowingWhyNeeded != null && optionalMessageForKnowingWhyNeeded.length > 0 && optionalMessageForKnowingWhyNeeded[0] != null) {
                    new AlertDialog.Builder(_activity)
                            .setMessage(optionalMessageForKnowingWhyNeeded[0])
                            .setCancelable(false)
                            .setNegativeButton(android.R.string.no, null)
                            .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                                ActivityCompat.requestPermissions(_activity, new String[]{Manifest.permission.POST_NOTIFICATIONS}, CODE_PERMISSION_POST_NOTIFICATIONS);
                            })
                            .show();
                    return false;
                }
                ActivityCompat.requestPermissions(_activity, new String[]{Manifest.permission.POST_NOTIFICATIONS}, CODE_PERMISSION_POST_NOTIFICATIONS);
                return false;
            }
        }
        return true;
    }

    public boolean checkPermissionResult(int requestCode, String[] permissions, int[] grantResults) {
        if (grantResults.length > 0) {
            switch (requestCode) {
                case CODE_PERMISSION_EXTERNAL_STORAGE:
                case CODE_PERMISSION_READ_MEDIA_IMAGES:
                case CODE_PERMISSION_POST_NOTIFICATIONS:
                    return grantResults[0] == PackageManager.PERMISSION_GRANTED;
            }
        }
        return false;
    }

    public boolean mkdirIfStoragePermissionGranted(File dir) {
        return doIfExtStoragePermissionGranted() && (dir.exists() || dir.mkdirs());
    }
}
