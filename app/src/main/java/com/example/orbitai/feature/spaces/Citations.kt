package com.example.orbitai.feature.spaces

import com.example.orbitai.feature.chat.MessageSource

/** Turns the Spaces chunks sent with a reply into the sources shown under it. */
object Citations {

    private val pageMarker = Regex("""\[Page (\d+)]""")

    /** First page a chunk covers, from the "[Page N]" markers added when PDFs are indexed. */
    fun page(content: String): Int? = pageMarker.find(content)?.groupValues?.get(1)?.toIntOrNull()

    /** The chunk text without page markers, shortened for display. */
    fun excerpt(content: String, maxLength: Int = 700): String {
        val clean = content.replace(pageMarker, "").replace(Regex("\\s+"), " ").trim()
        return if (clean.length <= maxLength) clean else clean.take(maxLength).substringBeforeLast(' ') + "…"
    }

    /** Sources for the chunks whose text was sent, numbered in prompt order ([sentContents]). */
    fun sources(retrieved: List<IndexChunk>, sentContents: List<String>): List<MessageSource> =
        sentContents.mapIndexedNotNull { index, content ->
            retrieved.firstOrNull { it.content == content }?.let { chunk ->
                MessageSource(
                    rank = index + 1,
                    chunkId = chunk.id,
                    docId = chunk.docId,
                    docName = chunk.docName,
                    page = page(chunk.content),
                    excerpt = excerpt(chunk.content),
                )
            }
        }

    /** Chip label, e.g. "1 · Q3-report.pdf · p.12". */
    fun label(source: MessageSource): String =
        listOfNotNull(source.rank.toString(), source.docName, source.page?.let { "p.$it" }).joinToString(" · ")
}
