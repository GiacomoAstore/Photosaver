package com.example.savemedia.models

import android.net.Uri

data class SavedMedia(
    val uri: Uri,
    val fileName: String,
    val timestamp: Long,
    val appName: String
)
