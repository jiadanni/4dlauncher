package com.jiadanni.launcher4d.feed

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Manages runtime permissions required for the feed feature
 */
class FeedPermissionManager(private val activity: Activity) {

    companion object {
        const val PERMISSION_REQUEST_CODE = 1001

        // Required permissions for feed
        val FEED_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_CALENDAR
        )

        /**
         * Check if all feed permissions are granted
         */
        fun hasAllPermissions(context: Context): Boolean {
            return FEED_PERMISSIONS.all { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        }

        /**
         * Check if location permissions are granted
         */
        fun hasLocationPermission(context: Context): Boolean {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }

        /**
         * Check if calendar permission is granted
         */
        fun hasCalendarPermission(context: Context): Boolean {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CALENDAR
            ) == PackageManager.PERMISSION_GRANTED
        }

        /**
         * Get list of missing permissions
         */
        fun getMissingPermissions(context: Context): List<String> {
            return FEED_PERMISSIONS.filter { permission ->
                ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
            }
        }

        /**
         * Get user-friendly permission names for display
         */
        fun getPermissionDisplayName(permission: String): String {
            return when (permission) {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION -> "Location"
                Manifest.permission.READ_CALENDAR -> "Calendar"
                else -> permission
            }
        }
    }

    /**
     * Request all required feed permissions
     */
    fun requestPermissions() {
        val missingPermissions = getMissingPermissions(activity)
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                missingPermissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    /**
     * Request only location permissions
     */
    fun requestLocationPermissions() {
        val locationPermissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        ActivityCompat.requestPermissions(
            activity,
            locationPermissions,
            PERMISSION_REQUEST_CODE
        )
    }

    /**
     * Request only calendar permission
     */
    fun requestCalendarPermission() {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.READ_CALENDAR),
            PERMISSION_REQUEST_CODE
        )
    }

    /**
     * Check if we should show rationale for any permission
     */
    fun shouldShowRationale(): Boolean {
        return FEED_PERMISSIONS.any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
    }

    /**
     * Check if we should show rationale for a specific permission
     */
    fun shouldShowRationale(permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }

    /**
     * Handle permission request results
     * Returns true if all requested permissions were granted
     */
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): PermissionResult {
        if (requestCode != PERMISSION_REQUEST_CODE) {
            return PermissionResult.NOT_HANDLED
        }

        val allGranted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }

        if (allGranted) {
            return PermissionResult.ALL_GRANTED
        }

        // Check which specific permissions were denied
        val deniedPermissions = mutableListOf<String>()
        permissions.forEachIndexed { index, permission ->
            if (grantResults.getOrNull(index) != PackageManager.PERMISSION_GRANTED) {
                deniedPermissions.add(permission)
            }
        }

        return PermissionResult.PARTIALLY_DENIED(deniedPermissions)
    }

    /**
     * Get a rationale message explaining why permissions are needed
     */
    fun getRationaleMessage(permissions: List<String>): String {
        val permissionNames = permissions.map { getPermissionDisplayName(it) }.distinct()

        return buildString {
            append("The feed feature needs access to:\n\n")

            if (permissionNames.contains("Location")) {
                append("• Location - for weather and travel time information\n")
            }
            if (permissionNames.contains("Calendar")) {
                append("• Calendar - to show upcoming events and travel times\n")
            }

            append("\nYou can change these permissions anytime in settings.")
        }
    }

    /**
     * Get a message for when permissions are denied
     */
    fun getDeniedMessage(deniedPermissions: List<String>): String {
        val permissionNames = deniedPermissions.map { getPermissionDisplayName(it) }.distinct()

        return buildString {
            append("Some feed features won't work without permissions:\n\n")

            if (permissionNames.contains("Location")) {
                append("• Weather and travel time cards require Location\n")
            }
            if (permissionNames.contains("Calendar")) {
                append("• Calendar event cards require Calendar access\n")
            }

            append("\nYou can enable them in Settings > Apps > 4D Launcher > Permissions")
        }
    }

    /**
     * Result of permission request
     */
    sealed class PermissionResult {
        object ALL_GRANTED : PermissionResult()
        object NOT_HANDLED : PermissionResult()
        data class PARTIALLY_DENIED(val deniedPermissions: List<String>) : PermissionResult()
    }
}
