package com.example.orbitai.tools.intents

import com.example.orbitai.feature.automation.executor.CallContact
import com.example.orbitai.feature.automation.executor.ContactNameMatcher
import org.junit.Assert.*
import org.junit.Test

class ContactNameMatcherTest {
    private val rahul = CallContact("Rahul Sharma", "+919876543210")

    @Test fun findsMisspelledFirstNameInsideFullContactName() {
        assertEquals(listOf(rahul), ContactNameMatcher.closest("Rhaul", listOf(rahul, CallContact("Priya", "9876543211"))))
        assertEquals(listOf(rahul), ContactNameMatcher.closest("Rahool", listOf(rahul)))
    }

    @Test fun normalizesAccentsCaseAndNameOrder() {
        val jose = CallContact("José García", "9876543210")
        assertEquals(listOf(jose), ContactNameMatcher.closest("  GARCIA, JOSE ", listOf(jose)))
    }

    @Test fun exactNameWinsOverFuzzyNeighbors() {
        val exact = CallContact("Rudra", "9876543210")
        assertEquals(listOf(exact), ContactNameMatcher.closest("Rudra", listOf(CallContact("Rudhra", "9876543211"), exact)))
    }

    @Test fun retainsAmbiguousNamesAndMultipleNumbers() {
        val other = CallContact("Rahul Verma", "9876543211")
        val work = rahul.copy(number = "9876543212")
        assertEquals(setOf(rahul, other, work), ContactNameMatcher.closest("Rahool", listOf(other, work, rahul)).toSet())
    }

    @Test fun similarPronunciationsRemainAmbiguous() {
        val rahil = CallContact("Rahil Sharma", "9876543213")
        assertEquals(setOf(rahul, rahil), ContactNameMatcher.closest("Ruhul", listOf(rahul, rahil)).toSet())
    }

    @Test fun rejectsUnrelatedNamesAndUnsafeNumbers() {
        assertTrue(ContactNameMatcher.closest("Zoe", listOf(rahul)).isEmpty())
        assertTrue(ContactNameMatcher.closest("", listOf(rahul)).isEmpty())
        assertTrue(ContactNameMatcher.closest("Mom", listOf(CallContact("Mom", "*21*9876543210#"))).isEmpty())
    }

    @Test fun removesDuplicateFormattedNumbers() {
        val mom = CallContact("Mom", "+919876543210")
        assertEquals(listOf(mom), ContactNameMatcher.closest("Mom", listOf(mom, mom.copy(number = "+91 98765 43210"))))
    }
}
