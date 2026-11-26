package com.example.orbitai.feature.voice

import org.junit.Assert.assertEquals
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
}
