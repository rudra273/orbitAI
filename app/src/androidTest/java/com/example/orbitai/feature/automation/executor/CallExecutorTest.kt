package com.example.orbitai.feature.automation.executor

import android.content.ActivityNotFoundException
import android.content.ContextWrapper
import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class CallExecutorTest {
    private val contact = CallContact("Test Contact", "+1 (234) 555-0123")
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun deviceWithoutTelephonyOpensTheDialerWithoutRequestingCallPermission() {
        var launched: Intent? = null
        val recordingContext = object : ContextWrapper(context) {
            override fun startActivity(intent: Intent) {
                launched = intent
            }
        }
        val executor = CallExecutor(recordingContext, supportsDirectCalls = false)

        assertFalse(executor.canCallDirectly(contact, dialOnly = false))
        assertNull(executor.launch(contact))
        assertEquals(Intent.ACTION_DIAL, launched?.action)
        assertEquals("+12345550123", launched?.data?.schemeSpecificPart)
    }

    @Test
    fun deviceWithoutADialerGetsAnError() {
        val contextWithoutDialer = object : ContextWrapper(context) {
            override fun startActivity(intent: Intent) {
                throw ActivityNotFoundException()
            }
        }
        val executor = CallExecutor(contextWithoutDialer, supportsDirectCalls = false)

        assertEquals("No phone app is available on this device.", executor.launch(contact))
    }
}
