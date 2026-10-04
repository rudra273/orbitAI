package com.example.orbitai.feature.spaces

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.FileNotFoundException

class DocumentIngestTest {

    private fun words(n: Int, prefix: String = "w") = (1..n).joinToString(" ") { "$prefix$it" }

    @Test
    fun smallBlocksShareOneChunk() {
        assertEquals(listOf("a b c d"), DocumentChunker.chunk(listOf("a b", "c d")))
        assertTrue(DocumentChunker.chunk(emptyList()).isEmpty())
        assertTrue(DocumentChunker.chunk(listOf("  ", "\n")).isEmpty())
    }

    @Test
    fun longTextIsSplitWithOverlap() {
        val chunks = DocumentChunker.chunk(listOf(words(1200)), chunkWordSize = 500, overlapWordCount = 125)
        assertTrue(chunks.all { it.split(" ").size <= 500 })
        // Every chunk after the first starts with the last 125 words of the previous one.
        chunks.zipWithNext().forEach { (a, b) ->
            assertEquals(a.split(" ").takeLast(125), b.split(" ").take(125))
        }
        assertEquals("w1200", chunks.last().split(" ").last())
    }

    @Test
    fun normalizeCollapsesWhitespace() {
        assertEquals("a b\nc\n\nd", DocumentChunker.normalize("  a \t b \n c\n\n\n\n d  "))
    }

    @Test
    fun failureReasonsAreReadable() {
        assertEquals(IngestFailure.SCANNED_PDF, IngestFailure.reasonFor(IngestException(IngestFailure.SCANNED_PDF)))
        assertTrue(IngestFailure.reasonFor(FileNotFoundException("x")).startsWith("File no longer accessible"))
        assertTrue(IngestFailure.reasonFor(SecurityException("denied")).startsWith("File no longer accessible"))
        assertTrue(IngestFailure.reasonFor(OutOfMemoryError()).startsWith("Too large"))
        assertTrue(IngestFailure.reasonFor(InvalidPasswordException("Cannot decrypt PDF, the password is incorrect")).startsWith("Password-protected"))
        assertTrue(IngestFailure.reasonFor(java.io.IOException("Error: Header doesn't contain versioninfo")).startsWith("The file looks damaged"))
        assertEquals("Couldn't read this file (IllegalStateException).", IngestFailure.reasonFor(IllegalStateException("boom")))
    }

    /** Stand-in with PDFBox's class name, which the classifier matches by name. */
    private class InvalidPasswordException(message: String) : java.io.IOException(message)
}
