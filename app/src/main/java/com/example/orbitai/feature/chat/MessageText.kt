package com.example.orbitai.feature.chat

/**
 * Markdown → plain text for copy and share: removes emphasis, heading and inline-code markers,
 * keeps fenced code blocks verbatim (without the fences) and keeps list markers.
 */
fun markdownToPlainText(markdown: String): String {
    val out = StringBuilder()
    var inCode = false
    for (line in markdown.lines()) {
        if (line.trimStart().startsWith("```")) {
            inCode = !inCode
            continue
        }
        if (inCode) {
            out.appendLine(line)
            continue
        }
        val plain = line
            .replace(Regex("^#{1,6}\\s+"), "")
            .replace(Regex("\\*\\*(.+?)\\*\\*"), "$1")
            .replace(Regex("__(.+?)__"), "$1")
            .replace(Regex("(?<![\\w*])\\*(?!\\s)(.+?)(?<!\\s)\\*(?![\\w*])"), "$1")
            .replace(Regex("`([^`]+)`"), "$1")
            .replace(Regex("^(\\s*)[*+]\\s+"), "$1- ")
        out.appendLine(plain)
    }
    return out.toString().trimEnd()
}
