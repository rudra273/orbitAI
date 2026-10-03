package com.example.orbitai.feature.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pins down the current regex behaviour, including known false positives that f037 will remove. */
class MemoryFactExtractorTest {

    @Test fun extractsExplicitPersonalFacts() {
        assertEquals(listOf("User's name is Rudra"), MemoryFactExtractor.extract("My name is Rudra"))
        assertEquals(listOf("User lives in Pune"), MemoryFactExtractor.extract("I live in Pune"))
        assertTrue(MemoryFactExtractor.extract("I work at Google").contains("User works at Google"))
    }

    @Test fun ordinaryQuestionsProduceNothing() {
        assertEquals(emptyList<String>(), MemoryFactExtractor.extract("What is the capital of France?"))
        assertEquals(emptyList<String>(), MemoryFactExtractor.extract("Explain Kotlin coroutines"))
    }

    @Test fun knownFalsePositivesBeforeF037() {
        assertEquals(listOf("User is a bit confused about this"), MemoryFactExtractor.extract("I am a bit confused about this"))
        assertEquals(listOf("User likes/loves you answer in bullets"), MemoryFactExtractor.extract("I prefer you answer in bullets"))
    }

    @Test fun overlongFactsAreDropped() {
        val long = "My name is " + "x".repeat(250)
        assertEquals(emptyList<String>(), MemoryFactExtractor.extract(long))
    }
}
