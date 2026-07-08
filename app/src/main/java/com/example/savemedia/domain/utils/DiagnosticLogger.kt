package com.example.savemedia.domain.utils

import android.content.Context
import android.os.Build
import com.example.savemedia.utils.AppLogger
import com.example.savemedia.utils.PermissionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiagnosticLogger @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: AppLogger,
    private val permissionManager: PermissionManager
) {
    fun logSystemInfo() {
        val info = mapOf(
            "Device" to Build.MODEL,
            "AndroidVersion" to Build.VERSION.RELEASE,
            "SDK" to Build.VERSION.SDK_INT.toString(),
            "StoragePermission" to permissionManager.hasStoragePermission().toString(),
            "OverlayPermission" to permissionManager.hasOverlayPermission().toString(),
            "AccessibilityPermission" to permissionManager.hasAccessibilityPermission().toString()
        )
        logger.i("System Diagnostics: $info", "Diagnostics")
    }
}
