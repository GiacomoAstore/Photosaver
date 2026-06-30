package com.example.savemedia.unit

import com.example.savemedia.utils.AppLogger
import com.example.savemedia.utils.LogLevel
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class AppLoggerTest {

    @Test
    fun `log should add entry to buffer`() {
        val context = RuntimeEnvironment.getApplication()
        val logger = AppLogger(context)

        logger.i("Test message", "TestComponent")

        val logs = logger.getRecentLogs(1)
        assertEquals(1, logs.size)
        assertEquals("Test message", logs[0].message)
        assertEquals(LogLevel.INFO, logs[0].level)
    }
}
