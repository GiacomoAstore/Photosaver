package com.example.savemedia.utils

import android.content.Context
import android.util.Log
import com.example.savemedia.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

enum class LogLevel {
    DEBUG, INFO, WARNING, ERROR, CRITICAL
}

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: Throwable? = null,
    val thread: String = Thread.currentThread().name,
    val sessionId: String,
    val component: String,
    val metadata: Map<String, String> = emptyMap()
)

class AppLogger(private val context: Context) {
    private val sessionId = UUID.randomUUID().toString()
    private val logBuffer = mutableListOf<LogEntry>()
    private val MAX_BUFFER_SIZE = 1000
    private val logDispatcher = Dispatchers.IO.limitedParallelism(1)
    private val scope = CoroutineScope(logDispatcher + SupervisorJob())
    private val TAG = "MediaSaver"

    private val _logFlow = MutableSharedFlow<LogEntry>(extraBufferCapacity = 64)
    fun observeLogs() = _logFlow.asSharedFlow()

    fun d(message: String, component: String, metadata: Map<String, String> = emptyMap()) {
        log(LogLevel.DEBUG, message, component, metadata)
    }

    fun i(message: String, component: String, metadata: Map<String, String> = emptyMap()) {
        log(LogLevel.INFO, message, component, metadata)
    }

    fun w(message: String, component: String, metadata: Map<String, String> = emptyMap()) {
        log(LogLevel.WARNING, message, component, metadata)
    }

    fun e(message: String, throwable: Throwable? = null, component: String, metadata: Map<String, String> = emptyMap()) {
        log(LogLevel.ERROR, message, component, metadata, throwable)
    }

    fun c(message: String, throwable: Throwable? = null, component: String, metadata: Map<String, String> = emptyMap()) {
        log(LogLevel.CRITICAL, message, component, metadata, throwable)
    }

    private fun log(level: LogLevel, message: String, component: String, metadata: Map<String, String>, throwable: Throwable? = null) {
        val entry = LogEntry(
            level = level,
            tag = TAG,
            message = message,
            throwable = throwable,
            sessionId = sessionId,
            component = component,
            metadata = metadata
        )

        if (BuildConfig.DEBUG) {
            val logMessage = "[$component] $message ${if (metadata.isNotEmpty()) "metadata=$metadata" else ""}"
            when (level) {
                LogLevel.DEBUG -> Log.d(TAG, logMessage)
                LogLevel.INFO -> Log.i(TAG, logMessage)
                LogLevel.WARNING -> Log.w(TAG, logMessage)
                LogLevel.ERROR -> Log.e(TAG, logMessage, throwable)
                LogLevel.CRITICAL -> Log.e(TAG, "CRITICAL: $logMessage", throwable)
            }
        }

        synchronized(logBuffer) {
            if (logBuffer.size >= MAX_BUFFER_SIZE) {
                logBuffer.removeAt(0)
            }
            logBuffer.add(entry)
        }

        scope.launch {
            _logFlow.emit(entry)
            persistLog(entry)
        }
    }

    private fun persistLog(entry: LogEntry) {
        try {
            val logFile = getLogFile()
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date(entry.timestamp))
            val line = "$timestamp | ${entry.level} | ${entry.component} | ${entry.message} | ${entry.metadata} | ${entry.throwable?.stackTraceToString() ?: ""}\n"

            // Simplified encryption: for demo purposes, we'll just write it.
            // In a real app, use encrypted file from security-crypto.
            logFile.appendText(line)

            // Check rotation
            if (logFile.length() > 10 * 1024 * 1024) { // 10MB
                rotateLogs()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist log", e)
        }
    }

    private fun getLogFile(): File {
        val logDir = File(context.filesDir, "logs")
        if (!logDir.exists()) logDir.mkdirs()
        return File(logDir, "current_log.txt")
    }

    private fun rotateLogs() {
        val logFile = getLogFile()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val archiveFile = File(logFile.parentFile, "log_$timestamp.txt")
        logFile.renameTo(archiveFile)
    }

    fun getRecentLogs(count: Int): List<LogEntry> {
        return synchronized(logBuffer) {
            logBuffer.takeLast(count)
        }
    }
}
