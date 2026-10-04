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

    /**
     * Feeds a reply that is still streaming and returns sentences as soon as they are complete,
     * so speech can start before the reply finishes.
     */
    class Stream {
        private var spoken = 0

        /** Sentences finished since the last call. With [final], also the trailing partial sentence. */
        fun next(markdownSoFar: String, final: Boolean = false): List<String> {
            val text = clean(markdownSoFar)
            if (text.length < spoken) spoken = text.length // Text was rewritten (e.g. markdown closed); don't repeat.
            val end = if (final) text.length else lastBoundary(text)
            if (end <= spoken) return emptyList()
            val chunk = text.substring(spoken, end)
            spoken = end
            return sentences(chunk)
        }

        private fun lastBoundary(text: String): Int {
            val match = Regex("""[.!?…](?=\s)|\n""").findAll(text).lastOrNull() ?: return 0
            return match.range.last + 1
        }
    }
}
