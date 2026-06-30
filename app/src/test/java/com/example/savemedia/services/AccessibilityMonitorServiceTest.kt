package com.example.savemedia.services

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.savemedia.utils.AppLogger
import com.example.savemedia.utils.CaptureBridge
import com.example.savemedia.utils.SmartThrottler
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class AccessibilityMonitorServiceTest {

    private lateinit var service: AccessibilityMonitorService
    private val logger = mockk<AppLogger>(relaxed = true)
    private val captureBridge = mockk<CaptureBridge>(relaxed = true)
    private val throttler = mockk<SmartThrottler>(relaxed = true)

    @Before
    fun setup() {
        service = Robolectric.buildService(AccessibilityMonitorService::class.java).get()
        service.logger = logger
        service.captureBridge = captureBridge
        service.throttler = throttler
        // By default allow captures
        every { throttler.shouldCapture() } returns true
    }

    @Test
    fun `isMessagingApp should return true for supported apps`() {
        assertTrue(service.isMessagingApp("com.whatsapp"))
        assertTrue(service.isMessagingApp("org.telegram.messenger"))
        assertTrue(service.isMessagingApp("com.instagram.android"))
    }

    @Test
    fun `isMessagingApp should return false for unsupported apps`() {
        assertFalse(service.isMessagingApp("com.android.settings"))
        assertFalse(service.isMessagingApp("com.google.android.youtube"))
    }

    @Test
    fun `detectViewOnce should return true when marker is present in WhatsApp`() {
        val rootNode = mockk<AccessibilityNodeInfo>()
        val childNode = mockk<AccessibilityNodeInfo>()

        every { rootNode.text } returns null
        every { rootNode.childCount } returns 1
        every { rootNode.getChild(0) } returns childNode

        every { childNode.text } returns "Foto" // WhatsApp marker
        every { childNode.childCount } returns 0

        assertTrue(service.detectViewOnce(rootNode, "com.whatsapp"))
    }

    @Test
    fun `detectViewOnce should return true when marker is present in Telegram`() {
        val rootNode = mockk<AccessibilityNodeInfo>()

        every { rootNode.text } returns "View Once" // Telegram marker
        every { rootNode.childCount } returns 0

        assertTrue(service.detectViewOnce(rootNode, "org.telegram.messenger"))
    }

    @Test
    fun `detectViewOnce should return false when marker is not present`() {
        val rootNode = mockk<AccessibilityNodeInfo>()

        every { rootNode.text } returns "Regular Message"
        every { rootNode.childCount } returns 0

        assertFalse(service.detectViewOnce(rootNode, "com.whatsapp"))
    }

    @Test
    fun `detectViewOnce should handle deep hierarchy`() {
        val rootNode = mockk<AccessibilityNodeInfo>()
        val level1 = mockk<AccessibilityNodeInfo>()
        val level2 = mockk<AccessibilityNodeInfo>()

        every { rootNode.text } returns null
        every { rootNode.childCount } returns 1
        every { rootNode.getChild(0) } returns level1

        every { level1.text } returns null
        every { level1.childCount } returns 1
        every { level1.getChild(0) } returns level2

        every { level2.text } returns "Vedi una volta" // Instagram marker
        every { level2.childCount } returns 0

        assertTrue(service.detectViewOnce(rootNode, "com.instagram.android"))
    }

    @Test
    fun `detectViewOnce should be case-insensitive`() {
        val rootNode = mockk<AccessibilityNodeInfo>()
        every { rootNode.text } returns "PHOTO" // Uppercase
        every { rootNode.childCount } returns 0

        assertTrue(service.detectViewOnce(rootNode, "com.whatsapp"))
    }

    @Test
    fun `onAccessibilityEvent should trigger capture when view once content is detected`() {
        val event = mockk<AccessibilityEvent>()
        every { event.eventType } returns AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        every { event.packageName } returns "com.whatsapp"
        every { event.className } returns ""   // must be mocked — strict mode

        val serviceSpy = spyk(service)
        // throttler is already set on service, spyk delegates to it
        val rootNode = mockk<AccessibilityNodeInfo>()

        // Mocking the rootInActiveWindow getter
        every { serviceSpy.rootInActiveWindow } returns rootNode

        // Simulating detection
        every { rootNode.text } returns "Foto"
        every { rootNode.childCount } returns 0

        every { captureBridge.hasPermission() } returns false  // avoid real FG service start

        serviceSpy.onAccessibilityEvent(event)

        verify { serviceSpy.triggerCapture("com.whatsapp") }
    }
}
