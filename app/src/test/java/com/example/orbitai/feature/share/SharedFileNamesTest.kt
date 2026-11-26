package com.example.orbitai.feature.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedFileNamesTest {

    @Test
    fun keepsReadableNamesAndExtensions() {
        assertEquals("Q3 report.pdf", SharedFileNames.safeName("Q3 report.pdf", "pdf"))
        assertEquals("IMG_2041.jpg", SharedFileNames.safeName("IMG_2041", "jpg"))
        assertEquals("passwd", SharedFileNames.safeName("../../etc/passwd", null))
        assertEquals("a_b_c.txt", SharedFileNames.safeName("a:b*c.txt", "txt"))
        assertEquals("shared.png", SharedFileNames.safeName("..", "png"))
        assertEquals("रिपोर्ट.pdf", SharedFileNames.safeName("रिपोर्ट.pdf", "pdf"))
    }

    @Test
    fun supportsPdfImagesAndText() {
        listOf("application/pdf", "image/jpeg", "image/png", "text/plain", "text/markdown", "application/json")
            .forEach { assertTrue(it, SharedFileNames.isSupported(it)) }
        listOf("video/mp4", "application/zip", "application/vnd.android.package-archive")
            .forEach { assertFalse(it, SharedFileNames.isSupported(it)) }
    }
}
