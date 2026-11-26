package com.example.orbitai.feature.automation.digest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DigestTest {

    private fun item(key: String, title: String, text: String, at: Long = 1_000_000L) = DigestItem(key, "WhatsApp", title, text, at)

    @Test
    fun codesAndMoneyMessagesAreSensitive() {
        listOf(
            "Your OTP is 482913", "482913 is your verification code", "Do not share this code 5543 with anyone",
            "Rs 2,000 debited from A/C XX1234", "UPI payment of 500 received", "आपका ओटीपी 123456 है",
            "Use 123456 to log in. Your password reset", "Your card ending 4242 was used",
        ).forEach { assertTrue(it, DigestFilter.isSensitive("Bank", it)) }
    }

    @Test
    fun ordinaryMessagesAreNot() {
        listOf("Are we still meeting at 5?", "Dinner on Saturday?", "Package delivered to your door", "कल शाम 6 बजे मिलते हैं")
            .forEach { assertFalse(it, DigestFilter.isSensitive("Friend", it)) }
    }

    @Test
    fun sensitiveNotificationsAreNeverStoredAndAreCounted() {
        val buffer = DigestBuffer(clock = { 1_000_500L })
        assertFalse(buffer.add(item("a", "Bank", "Your OTP is 123456")))
        assertTrue(buffer.add(item("b", "Aditya", "lunch?")))
        assertEquals(1, buffer.size)
        assertEquals(1, buffer.skippedSensitive)
    }

    @Test
    fun sameKeyReplacesAndOldItemsExpire() {
        var now = 100L * 24 * 60 * 60 * 1000
        val buffer = DigestBuffer(clock = { now })
        buffer.add(item("a", "Aditya", "first", at = now))
        buffer.add(item("a", "Aditya", "second", at = now))
        assertEquals(listOf("second"), buffer.snapshot().map { it.text })
        now += 25L * 60 * 60 * 1000
        assertTrue(buffer.snapshot().isEmpty())
    }

    @Test
    fun bufferIsBounded() {
        val buffer = DigestBuffer(maxItems = 3, clock = { 1_000_500L })
        (1..6).forEach { buffer.add(item("k$it", "T", "m$it", at = it.toLong() + 1_000_000L)) }
        assertEquals(listOf("m4", "m5", "m6"), buffer.snapshot().map { it.text })
    }

    @Test
    fun promptListsNewestFirstAndStaysShort() {
        val items = (1..400).map { item("k$it", "Person $it", "message number $it with some padding text", at = it.toLong()) }
        val prompt = DigestPrompt.build(items)
        assertTrue(prompt.length < 7000)
        assertTrue(prompt.indexOf("Person 400") in 0 until prompt.indexOf("Person 399"))
        assertFalse(prompt.contains("Person 1:"))
    }

    @Test
    fun emptyNotificationsAreIgnored() {
        assertFalse(DigestBuffer().add(item("a", " ", " ")))
    }
}
