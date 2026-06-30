package com.example.savemedia.unit

import android.content.Context
import com.example.savemedia.utils.FileManager
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Test

class FileManagerTest {

    @Test
    fun fileManager_shouldBeInstantiable() {
        val context = mockk<Context>(relaxed = true)
        val sanitizer = mockk<com.example.savemedia.utils.DataSanitizer>(relaxed = true)
        val encryption = mockk<com.example.savemedia.utils.MediaEncryption>(relaxed = true)
        val fileManager = FileManager(context, sanitizer, encryption)
        assertNotNull(fileManager)
    }
}
