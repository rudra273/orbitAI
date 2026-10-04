package com.example.orbitai.feature.spaces.embedding

import com.google.gson.JsonParser
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File

class UnigramTokenizerTest {

    /** A tiny vocabulary: enough to check normalisation, word marking, scoring and truncation. */
    private val tiny = UnigramTokenizer.load(
        ByteArrayInputStream(
            """{"model":{"type":"Unigram","unk_id":3,"vocab":[["<s>",0],["<pad>",0],["</s>",0],["<unk>",0],
            ["▁",-2.0],["▁hello",-3.0],["▁hell",-5.0],["o",-4.0],["▁world",-3.0],["w",-6.0],["▁w",-6.0],["orld",-5.0]]}}""".toByteArray(),
        ),
    )

    @Test
    fun wordsAreMarkedAndWrappedInSpecialTokens() {
        assertArrayEquals(intArrayOf(0, 5, 8, 2), tiny.encode("hello  world"))
        assertArrayEquals(intArrayOf(0, 4, 2), tiny.encode("   "))
        assertArrayEquals(intArrayOf(0, 2), tiny.encode(""))
    }

    @Test
    fun theHigherScoringSplitWins() {
        // "▁hello" (-3) beats "▁hell" + "o" (-9).
        assertArrayEquals(intArrayOf(0, 5, 2), tiny.encode("hello"))
    }

    @Test
    fun unknownCharactersBecomeUnk() {
        assertEquals(3, tiny.encode("hello ☃")[3])
    }

    @Test
    fun longTextIsCutButKeepsTheClosingToken() {
        val ids = tiny.encode("hello ".repeat(100), maxTokens = 10)
        assertEquals(10, ids.size)
        assertEquals(0, ids.first())
        assertEquals(2, ids.last())
    }

    /**
     * Compares with ids from the Hugging Face tokenizer for 23 texts (English, Hindi, Tamil, Gujarati,
     * Japanese, symbols). Needs the real `tokenizer.json`: set ORBIT_E5_TOKENIZER to its path.
     */
    @Test
    fun matchesTheReferenceTokenizer() {
        val path = System.getenv("ORBIT_E5_TOKENIZER")
        assumeTrue("ORBIT_E5_TOKENIZER not set", path != null && File(path).exists())
        val tokenizer = UnigramTokenizer.load(File(path!!))
        val cases = JsonParser.parseReader(javaClass.getResourceAsStream("/e5_tokenizer_reference.json")!!.reader()).asJsonArray
        val mismatches = cases.mapNotNull { element ->
            val obj = element.asJsonObject
            val expected = obj["ids"].asJsonArray.map { it.asInt }
            val actual = tokenizer.encode(obj["text"].asString, maxTokens = Int.MAX_VALUE).toList()
            if (expected == actual) null else obj["text"].asString.take(40) to (expected to actual)
        }
        assertTrue("Mismatches: ${mismatches.joinToString("\n")}", mismatches.isEmpty())
    }
}
