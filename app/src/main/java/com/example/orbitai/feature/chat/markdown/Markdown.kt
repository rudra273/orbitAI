package com.example.orbitai.feature.chat.markdown

/** Block-level markdown elements produced by [MarkdownParser]. */
sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    /** [number] is set for ordered items; [checked] is set for task-list items. */
    data class ListItem(val depth: Int, val number: Int?, val checked: Boolean?, val text: String) : MdBlock
    data class Quote(val text: String) : MdBlock
    /** [closed] is false while the closing fence hasn't streamed in yet. */
    data class Code(val language: String, val code: String, val closed: Boolean) : MdBlock
    data class Table(val header: List<String>, val alignments: List<Align>, val rows: List<List<String>>) : MdBlock
    data object Rule : MdBlock
}

enum class Align { START, CENTER, END }

/** A run of inline text with uniform styling. [url] is set for links. */
data class MdSpan(
    val text: String,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val strike: Boolean = false,
    val code: Boolean = false,
    val url: String? = null,
)

/**
 * Small CommonMark-ish parser for chat replies (plus GFM tables, task lists and strikethrough).
 * Tolerates partial input while a reply streams: an unclosed fence becomes an open code block and
 * a table header without its separator row stays a paragraph until the separator arrives.
 */
object MarkdownParser {

    private val heading = Regex("^(#{1,6})\\s+(.+?)\\s*#*\\s*$")
    private val rule = Regex("^\\s{0,3}([-*_])(\\s*\\1){2,}\\s*$")
    private val unordered = Regex("^(\\s*)[-*+]\\s+(.*)$")
    private val ordered = Regex("^(\\s*)(\\d{1,9})[.)]\\s+(.*)$")
    private val task = Regex("^\\[([ xX])]\\s+(.*)$")
    private val quote = Regex("^\\s{0,3}>\\s?(.*)$")
    private val tableSeparator = Regex("^\\s*\\|?\\s*:?-{3,}:?\\s*(\\|\\s*:?-{3,}:?\\s*)*\\|?\\s*$")

    fun parseBlocks(content: String): List<MdBlock> {
        val blocks = mutableListOf<MdBlock>()
        val paragraph = mutableListOf<String>()
        val lines = content.replace("\r\n", "\n").lines()
        var i = 0

        fun flushParagraph() {
            if (paragraph.isNotEmpty()) {
                blocks += MdBlock.Paragraph(paragraph.joinToString("\n").trim())
                paragraph.clear()
            }
        }

        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trimStart()

            if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
                flushParagraph()
                val fence = trimmed.take(3)
                val language = trimmed.drop(3).trim()
                val code = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].trimStart().startsWith(fence)) {
                    code += lines[i]
                    i++
                }
                val closed = i < lines.size
                if (closed) i++
                blocks += MdBlock.Code(language, code.joinToString("\n"), closed)
                continue
            }

            if (line.contains('|') && i + 1 < lines.size && tableSeparator.matches(lines[i + 1]) && lines[i + 1].contains('-')) {
                flushParagraph()
                val header = splitRow(line)
                val alignments = splitRow(lines[i + 1]).map { cell ->
                    val c = cell.trim()
                    when {
                        c.startsWith(":") && c.endsWith(":") -> Align.CENTER
                        c.endsWith(":") -> Align.END
                        else -> Align.START
                    }
                }
                i += 2
                val rows = mutableListOf<List<String>>()
                while (i < lines.size && lines[i].contains('|') && lines[i].isNotBlank()) {
                    rows += splitRow(lines[i]).let { cells ->
                        // Pad or trim to the header width so every row lines up.
                        List(header.size) { col -> cells.getOrElse(col) { "" } }
                    }
                    i++
                }
                blocks += MdBlock.Table(header, List(header.size) { alignments.getOrElse(it) { Align.START } }, rows)
                continue
            }

            val headingMatch = heading.find(line)
            when {
                line.isBlank() -> flushParagraph()
                headingMatch != null -> {
                    flushParagraph()
                    blocks += MdBlock.Heading(headingMatch.groupValues[1].length, headingMatch.groupValues[2])
                }
                rule.matches(line) -> {
                    flushParagraph()
                    blocks += MdBlock.Rule
                }
                quote.matches(line) -> {
                    flushParagraph()
                    val quoted = mutableListOf<String>()
                    while (i < lines.size && quote.matches(lines[i])) {
                        quoted += quote.find(lines[i])!!.groupValues[1]
                        i++
                    }
                    blocks += MdBlock.Quote(quoted.joinToString("\n").trim())
                    continue
                }
                ordered.matches(line) -> {
                    flushParagraph()
                    val m = ordered.find(line)!!
                    blocks += listItem(m.groupValues[1], m.groupValues[2].toIntOrNull(), m.groupValues[3])
                }
                unordered.matches(line) -> {
                    flushParagraph()
                    val m = unordered.find(line)!!
                    blocks += listItem(m.groupValues[1], null, m.groupValues[2])
                }
                else -> paragraph += line
            }
            i++
        }
        flushParagraph()
        return blocks
    }

    private fun listItem(indent: String, number: Int?, rest: String): MdBlock.ListItem {
        val depth = (indent.replace("\t", "    ").length / 2).coerceAtMost(3)
        val taskMatch = if (number == null) task.find(rest) else null
        return if (taskMatch != null) {
            MdBlock.ListItem(depth, null, taskMatch.groupValues[1].isNotBlank(), taskMatch.groupValues[2])
        } else {
            MdBlock.ListItem(depth, number, null, rest)
        }
    }

    private fun splitRow(line: String): List<String> =
        line.trim().removePrefix("|").removeSuffix("|").split(Regex("(?<!\\\\)\\|")).map { it.trim().replace("\\|", "|") }

    // ── Inline ────────────────────────────────────────────────────────────────

    private val inline = Regex(
        listOf(
            "(`+)(.+?)\\1",                                   // 1,2  code
            "\\[([^\\]]+)]\\((https?://[^)\\s]+)\\)",         // 3,4  [text](url)
            "(https?://[^\\s<>()]+[^\\s<>().,;:!?\"'])",      // 5    bare url
            "\\*\\*(.+?)\\*\\*(?!\\*)|__(.+?)__(?!_)",        // 6,7  bold (closing run may also end an italic)
            "~~(.+?)~~",                                      // 8    strike
            "(?<![\\w*])\\*(?![\\s*])(.+?)(?<![\\s*])\\*(?![\\w*])", // 9 italic *x*
            "(?<![\\w_])_(?![\\s_])(.+?)(?<![\\s_])_(?![\\w_])",     // 10 italic _x_
        ).joinToString("|")
    )

    fun parseInline(text: String): List<MdSpan> {
        val spans = mutableListOf<MdSpan>()
        var last = 0
        for (m in inline.findAll(text)) {
            if (m.range.first > last) spans += MdSpan(text.substring(last, m.range.first))
            val g = m.groups
            spans += when {
                g[2] != null -> listOf(MdSpan(g[2]!!.value, code = true))
                g[3] != null -> listOf(MdSpan(g[3]!!.value, url = g[4]!!.value))
                g[5] != null -> listOf(MdSpan(g[5]!!.value, url = g[5]!!.value))
                // Bold may contain italics or code, e.g. **see *this***.
                g[6] != null || g[7] != null -> parseInline((g[6] ?: g[7])!!.value).map { it.copy(bold = true) }
                g[8] != null -> parseInline(g[8]!!.value).map { it.copy(strike = true) }
                else -> listOf(MdSpan((g[9] ?: g[10])!!.value, italic = true))
            }
            last = m.range.last + 1
        }
        if (last < text.length) spans += MdSpan(text.substring(last))
        return spans
    }
}

/** Converts parsed markdown to the simple HTML subset Android's `Html.fromHtml` understands (used by the bubble). */
object MarkdownHtml {

    fun toHtml(content: String): String = MarkdownParser.parseBlocks(content).joinToString("") { block ->
        when (block) {
            is MdBlock.Heading -> "<p><b>${inline(block.text)}</b></p>"
            is MdBlock.Paragraph -> "<p>${inline(block.text).replace("\n", "<br>")}</p>"
            is MdBlock.ListItem -> {
                val marker = when {
                    block.checked == true -> "☑"
                    block.checked == false -> "☐"
                    block.number != null -> "${block.number}."
                    else -> "•"
                }
                "&nbsp;".repeat(block.depth * 4) + "$marker ${inline(block.text)}<br>"
            }
            is MdBlock.Quote -> "<blockquote>${inline(block.text).replace("\n", "<br>")}</blockquote>"
            is MdBlock.Code -> "<p><tt>${escape(block.code).replace("\n", "<br>")}</tt></p>"
            is MdBlock.Table -> (listOf(block.header) + block.rows).joinToString("") { row ->
                row.joinToString(" | ") { inline(it) } + "<br>"
            }
            MdBlock.Rule -> "<p>────────</p>"
        }
    }

    private fun inline(text: String): String = MarkdownParser.parseInline(text).joinToString("") { span ->
        var out = escape(span.text)
        if (span.code) out = "<tt>$out</tt>"
        if (span.bold) out = "<b>$out</b>"
        if (span.italic) out = "<i>$out</i>"
        if (span.strike) out = "<s>$out</s>"
        if (span.url != null) out = "<a href=\"${escape(span.url)}\">$out</a>"
        out
    }

    fun escape(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
