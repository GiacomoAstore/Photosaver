package com.example.savemedia.utils

import android.content.Context
import javax.inject.Inject

class HealthReport(val issues: List<String>) {
    val isHealthy: Boolean = issues.isEmpty()
}

class HealthChecker @Inject constructor(
    private val context: Context,
    private val logger: AppLogger
) {
    fun checkAll(): HealthReport {
        val issues = mutableListOf<String>()

        // Check Storage
        val freeSpace = context.filesDir.freeSpace / (1024 * 1024)
        if (freeSpace < 100) {
            issues.add("Low storage: ${freeSpace}MB")
        }

        if (issues.isNotEmpty()) {
            logger.w("Health check found issues", "HealthChecker", mapOf("issues" to issues.toString()))
        }

        return HealthReport(issues)
    }
}
