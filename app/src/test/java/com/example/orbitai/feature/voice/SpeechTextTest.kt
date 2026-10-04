package com.example.orbitai.feature.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechTextTest {

    @Test
    fun cleanDropsCodeLinksAndSyntax() {
        val markdown = """
            ## Steps
            Run **this** first, see [the docs](https://example.com/docs).
            ```kotlin
            fun main() = println("hi")
            ```
            - Install it
            - Visit https://kotlinlang.org
            | Name | Age |
            |------|-----|
            | Ana  | 30  |
        """.trimIndent()
        assertEquals(
            listOf("Steps", "Run this first, see the docs.", "Code is shown on screen.", "Install it", "Visit link", "Name, Age", "Ana, 30"),
            SpeechText.sentences(SpeechText.clean(markdown)),
        )
    }

    @Test
    fun streamSpeaksCompleteSentencesEarly() {
        val stream = SpeechText.Stream()
        assertEquals(emptyList<String>(), stream.next("Hello there"))
        assertEquals(listOf("Hello there."), stream.next("Hello there. How a"))
        assertEquals(emptyList<String>(), stream.next("Hello there. How are"))
        assertEquals(listOf("How are you?"), stream.next("Hello there. How are you? I am"))
        assertEquals(listOf("I am fine"), stream.next("Hello there. How are you? I am fine", final = true))
        assertEquals(emptyList<String>(), stream.next("Hello there. How are you? I am fine", final = true))
    }

    @Test
    fun streamSkipsCodeWhileItStreams() {
        val stream = SpeechText.Stream()
        val spoken = mutableListOf<String>()
        val reply = "Here you go.\n```\nval x = 1\nval y = 2\n```\nThat's it."
        for (end in 1..reply.length) spoken += stream.next(reply.substring(0, end))
        spoken += stream.next(reply, final = true)
        assertTrue(spoken.toString(), spoken.none { "val" in it })
        assertEquals(listOf("Here you go.", "Code is shown on screen.", "That's it."), spoken)
    }
}
