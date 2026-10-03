package com.example.orbitai.feature.chat

import org.junit.Assert.assertEquals
import org.junit.Test

class MessageTextTest {

    @Test fun stripsEmphasisHeadingsAndInlineCode() {
        assertEquals(
            "Title\nThis is bold and italic with code.",
            markdownToPlainText("## Title\nThis is **bold** and *italic* with `code`."),
        )
    }

    @Test fun keepsCodeBlocksVerbatimWithoutFences() {
        val md = "Run:\n```kotlin\nval x = a * b * c\nprintln(\"**not bold**\")\n```\nDone"
        assertEquals("Run:\nval x = a * b * c\nprintln(\"**not bold**\")\nDone", markdownToPlainText(md))
    }

    @Test fun normalisesBulletsAndKeepsNumbers() {
        assertEquals("- one\n- two\n1. first", markdownToPlainText("* one\n+ two\n1. first"))
    }

    @Test fun leavesArithmeticAndSnakeCaseAlone() {
        assertEquals("2 * 3 = 6 and my_var_name", markdownToPlainText("2 * 3 = 6 and my_var_name"))
    }
}
