package com.example.savemedia.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionManager @Inject constructor(
    private val context: Context,
    private val logger: AppLogger
) {
    fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun hasAccessibilityPermission(): Boolean {
        val accessibilityEnabled = Settings.Secure.getInt(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0)
        if (accessibilityEnabled == 1) {
            val serviceName = "${context.packageName}/com.example.savemedia.services.AccessibilityMonitorService"
            val settingValue = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            
            // Check both full name and simple class name for Samsung compatibility
            return settingValue?.contains(context.packageName) == true || 
                   settingValue?.contains("AccessibilityMonitorService") == true
        }
        return false
    }

    fun requestStoragePermission(activity: Activity, requestCode: Int) {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        ActivityCompat.requestPermissions(activity, permissions, requestCode)
    }

    fun requestOverlayPermission(activity: Activity) {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
        activity.startActivity(intent)
    }

    fun requestAccessibilityPermission(activity: Activity) {
        try {
            // Try to open the specific service settings page directly
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            val componentName = "${context.packageName}/com.example.savemedia.services.AccessibilityMonitorService"
            intent.putExtra(":settings:show_fragment_args_key", componentName)
            intent.putExtra(":settings:fragment_args_key", componentName)
            activity.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to general accessibility settings
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            activity.startActivity(intent)
        }
    }

    fun openAppSettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:${context.packageName}")
        activity.startActivity(intent)
    }
}
