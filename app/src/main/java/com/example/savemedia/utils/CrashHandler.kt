package com.example.savemedia.utils

import android.content.Context
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrashHandler @Inject constructor(
    private val logger: AppLogger,
    private val context: Context
) : Thread.UncaughtExceptionHandler {
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        logger.c("Uncaught exception in thread ${thread.name}", throwable, "CrashHandler")

        val report = """
            Time: ${System.currentTimeMillis()}
            Thread: ${thread.name}
            Exception: ${throwable.javaClass.simpleName}
            Message: ${throwable.message}
            Stacktrace: ${throwable.stackTraceToString()}
        """.trimIndent()

        saveCrashReport(report)

        defaultHandler?.uncaughtException(thread, throwable)
    }

    private fun saveCrashReport(report: String) {
        try {
            val crashDir = File(context.filesDir, "crashes")
            if (!crashDir.exists()) crashDir.mkdirs()
            File(crashDir, "crash_${System.currentTimeMillis()}.txt").writeText(report)
        } catch (e: Exception) {
            // Can't do much here
        }
    }
}
