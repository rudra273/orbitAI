package com.example.orbitai.feature.spaces

import java.io.InputStream
import java.util.zip.ZipInputStream

/** Plain text from Word (.docx) and e-book (.epub) files, which are zip archives of XML/XHTML. */
object ArchiveText {

    const val DOCX_MIME = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    const val EPUB_MIME = "application/epub+zip"
    private const val MAX_ENTRY_BYTES = 20 * 1024 * 1024

    fun isArchiveDocument(mimeType: String) = mimeType == DOCX_MIME || mimeType == EPUB_MIME

    /** Paragraphs of `word/document.xml`, in order. */
    fun docxParagraphs(input: InputStream): List<String> {
        var xml: String? = null
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name == "word/document.xml") { xml = readLimited(zip); break }
            }
        }
        val document = xml ?: return emptyList()
        return Regex("""(?s)<w:p[ >].*?</w:p>""").findAll(document).map { paragraph ->
            val text = paragraph.value
                .replace(Regex("""<w:tab\s*/>"""), "\t")
                .replace(Regex("""<w:br\s*/>"""), "\n")
                .let { p -> Regex("""(?s)<w:t(?:\s[^>]*)?>(.*?)</w:t>|(\t)|(\n)""").findAll(p).joinToString("") { it.groupValues[1].ifEmpty { it.groupValues[2] + it.groupValues[3] } } }
            decodeEntities(text).trim()
        }.filter { it.isNotBlank() }.toList()
    }

    /** One block per chapter file, in the order the archive lists them. */
    fun epubChapters(input: InputStream): List<String> {
        val chapters = mutableListOf<Pair<String, String>>()
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name.lowercase()
                if (!entry.isDirectory && (name.endsWith(".xhtml") || name.endsWith(".html") || name.endsWith(".htm"))) {
                    chapters += entry.name to readLimited(zip)
                }
            }
        }
        return chapters.sortedBy { it.first }.map { htmlToText(it.second) }.filter { it.isNotBlank() }
    }

    internal fun htmlToText(html: String): String {
        val body = Regex("""(?is)<body[^>]*>(.*)</body>""").find(html)?.groupValues?.get(1) ?: html
        return decodeEntities(
            body.replace(Regex("""(?is)<(script|style)[^>]*>.*?</\1>"""), " ")
                .replace(Regex("""(?i)</(p|div|h[1-6]|li|tr|blockquote)>|<br\s*/?>"""), "\n\n")
                .replace(Regex("""<[^>]+>"""), ""),
        ).lines().joinToString("\n") { it.trim() }.replace(Regex("\n{3,}"), "\n\n").trim()
    }

    private fun decodeEntities(text: String): String = text
        .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'")
        .replace("&nbsp;", " ")
        .replace(Regex("""&#(\d+);""")) { it.groupValues[1].toIntOrNull()?.let { c -> String(Character.toChars(c)) } ?: it.value }
        .replace(Regex("""&#x([0-9a-fA-F]+);""")) { it.groupValues[1].toIntOrNull(16)?.let { c -> String(Character.toChars(c)) } ?: it.value }
        .replace("&amp;", "&")

    private fun readLimited(zip: ZipInputStream): String {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val n = zip.read(buffer)
            if (n < 0) break
            out.write(buffer, 0, n)
            if (out.size() > MAX_ENTRY_BYTES) break
        }
        return out.toString(Charsets.UTF_8.name())
    }
}
