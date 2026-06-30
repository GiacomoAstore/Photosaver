package com.example.savemedia.unit

import android.content.Context
import android.graphics.Bitmap
import com.example.savemedia.utils.AppLogger
import com.example.savemedia.utils.DataSanitizer
import com.example.savemedia.utils.FileManager
import com.example.savemedia.utils.MediaEncryption
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class FileManagerTest {

    private lateinit var fileManager: FileManager
    private val context = mockk<Context>(relaxed = true)
    private val sanitizer = mockk<DataSanitizer>(relaxed = true)
    private val encryption = mockk<MediaEncryption>(relaxed = true)
    private val logger = mockk<AppLogger>(relaxed = true)

    @Before
    fun setup() {
        fileManager = FileManager(context, sanitizer, encryption, logger)
    }

    @Test
    fun `fileManager should be instantiable`() {
        assertNotNull(fileManager)
    }

    @Test
    fun `saveBitmap should return null when contentResolver insert fails`() {
        val bitmap = mockk<Bitmap>(relaxed = true)
        // ContentResolver returns null on insert (simulates permission denied / MediaStore error)
        every { context.contentResolver.insert(any(), any()) } returns null

        val result = fileManager.saveBitmap(bitmap, "TestApp")

        assertNull(result)
    }
}
