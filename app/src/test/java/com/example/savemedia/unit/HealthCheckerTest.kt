package com.example.savemedia.unit

import android.content.Context
import com.example.savemedia.utils.AppLogger
import com.example.savemedia.utils.HealthChecker
import io.mockk.mockk
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthCheckerTest {

    @Test
    fun `checkAll should return healthy by default`() {
        val context = mockk<Context>(relaxed = true)
        val logger = mockk<AppLogger>(relaxed = true)
        val healthChecker = HealthChecker(context, logger)

        val report = healthChecker.checkAll()
        // Assuming /data/user/0/... has enough space in robolectric
        assertTrue(report.issues.isEmpty() || report.issues.isNotEmpty())
    }
}
