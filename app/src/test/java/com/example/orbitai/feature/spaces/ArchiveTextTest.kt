package com.example.orbitai.feature.spaces

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ArchiveTextTest {

    private fun zip(vararg files: Pair<String, String>): ByteArrayInputStream {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { z -> files.forEach { (n, c) -> z.putNextEntry(ZipEntry(n)); z.write(c.toByteArray()); z.closeEntry() } }
        return ByteArrayInputStream(out.toByteArray())
    }

    @Test
    fun docxParagraphsJoinRunsAndDecodeEntities() {
        val xml = """<?xml version="1.0"?><w:document><w:body>
            <w:p><w:r><w:t>Hello </w:t></w:r><w:r><w:t xml:space="preserve">world &amp; co</w:t></w:r></w:p>
            <w:p><w:pPr/></w:p>
            <w:p><w:r><w:t>नमस्ते</w:t></w:r><w:r><w:tab/></w:r><w:r><w:t>दुनिया</w:t></w:r></w:p>
            </w:body></w:document>"""
        val paragraphs = ArchiveText.docxParagraphs(zip("word/document.xml" to xml))
        assertEquals(listOf("Hello world & co", "नमस्ते\tदुनिया"), paragraphs)
    }

    @Test
    fun docxWithoutDocumentXmlIsEmpty() {
        assertTrue(ArchiveText.docxParagraphs(zip("other.xml" to "<a/>")).isEmpty())
    }

    @Test
    fun epubChaptersAreStrippedAndOrdered() {
        val chapter2 = "<html><head><style>p{}</style></head><body><h1>Two</h1><p>Second &lt;chapter&gt;</p></body></html>"
        val chapter1 = "<html><body><h1>One</h1><p>First&nbsp;chapter</p><script>x()</script></body></html>"
        val chapters = ArchiveText.epubChapters(zip("OEBPS/ch2.xhtml" to chapter2, "OEBPS/ch1.xhtml" to chapter1, "mimetype" to "application/epub+zip"))
        assertEquals(2, chapters.size)
        assertEquals("One\n\nFirst chapter", chapters[0])
        assertEquals("Two\n\nSecond <chapter>", chapters[1])
    }
}
