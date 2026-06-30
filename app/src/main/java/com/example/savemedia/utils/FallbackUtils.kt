package com.example.savemedia.utils

import android.content.Context
import android.widget.Toast
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserFriendlyFallback @Inject constructor(
    private val context: Context,
    private val logger: AppLogger
) {
    fun handleCaptureFailure(error: Throwable, retryCount: Int) {}
}

@Singleton
class ManualCaptureHelper @Inject constructor(
    private val logger: AppLogger,
    private val context: Context
) {
    fun activateManualMode() {}
}
