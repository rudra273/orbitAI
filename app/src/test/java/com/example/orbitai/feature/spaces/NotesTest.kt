package com.example.orbitai.feature.spaces

import org.junit.Assert.assertEquals
import org.junit.Test

class NotesTest {
    @Test
    fun displayNameIsPrefixedTrimmedAndNeverEmpty() {
        assertEquals("Note: Trip plan", Notes.displayName("  Trip   plan "))
        assertEquals("Note: Untitled", Notes.displayName("   "))
        assertEquals("Note: " + "a".repeat(60), Notes.displayName("a".repeat(100)))
    }
}
