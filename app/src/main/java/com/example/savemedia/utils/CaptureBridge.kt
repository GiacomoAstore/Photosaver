package com.example.savemedia.utils

import android.content.Intent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CaptureBridge @Inject constructor() {
    var captureIntent: Intent? = null
    var resultCode: Int = 0

    fun hasPermission() = captureIntent != null && resultCode != 0
}
