package com.example.orbitai.feature.voice

import com.example.orbitai.feature.chat.markdownToPlainText

/** Turns assistant markdown into short sentences for text-to-speech. */
object SpeechText {

    private const val CODE_NOTE = "Code is shown on screen."

    /** Removes what shouldn't be read aloud: code blocks, link targets, markdown syntax, table rules. */
    fun clean(markdown: String): String {
        val withoutCode = Regex("```[\\s\\S]*?(```|$)").replace(markdown) { "\n$CODE_NOTE\n" }
        val plain = markdownToPlainText(
            withoutCode
                .replace(Regex("""!\[[^\]]*]\([^)]*\)"""), "")
                .replace(Regex("""\[([^\]]+)]\([^)]*\)"""), "$1"),
        )
        return plain.lines().joinToString("\n") { line ->
            line
                .replace(Regex("""https?://\S+"""), "link")
                .replace(Regex("""^\s*\|?[-:| ]{3,}\|?\s*$"""), "")
                .replace(Regex("""^\s*[-*+]\s+"""), "")
                .replace(Regex("""\s*\|\s*"""), ", ")
                .replace(Regex("""^>\s?"""), "")
                .trim(' ', ',')
        }.replace(Regex("\n{2,}"), "\n").trim()
    }

    /** Splits clean text into sentences; lines (list items, headings) count as sentences too. */
    fun sentences(text: String): List<String> =
        text.split(Regex("""(?<=[.!?…])\s+|\n+""")).map { it.trim() }.filter { it.any(Char::isLetterOrDigit) }
}
