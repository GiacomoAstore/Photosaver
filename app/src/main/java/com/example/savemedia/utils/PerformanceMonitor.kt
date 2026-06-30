package com.example.savemedia.utils

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

data class PerformanceMetrics(
    val operation: String,
    val duration: Long,
    val average: Double
)

@Singleton
class PerformanceMonitor @Inject constructor(private val logger: AppLogger) {
    private val metrics = mutableMapOf<String, MutableList<Long>>()

    private val _metricsFlow = MutableSharedFlow<PerformanceMetrics>(extraBufferCapacity = 16)
    fun observeMetrics() = _metricsFlow.asSharedFlow()

    fun measureTime(operation: String, block: () -> Unit) {
        val start = System.currentTimeMillis()
        block()
        val duration = System.currentTimeMillis() - start
        val avg = getAverage(operation)

        metrics.getOrPut(operation) { mutableListOf() }.add(duration)

        _metricsFlow.tryEmit(PerformanceMetrics(operation, duration, avg))

        logger.d("Performance: $operation took ${duration}ms", "Performance",
            mapOf("avg" to avg.toString()))

        if (duration > 1000) {
            logger.w("Slow execution detected: $operation ($duration ms)", "Performance")
        }
    }

    private fun getAverage(operation: String): Double {
        return metrics[operation]?.average() ?: 0.0
    }
}
