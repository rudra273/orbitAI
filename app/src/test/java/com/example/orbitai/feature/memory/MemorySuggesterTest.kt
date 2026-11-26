package com.example.orbitai.feature.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MemorySuggesterTest {

    @Test
    fun everydaySentencesSuggestNothing() {
        val negatives = listOf(
            "I am a bit confused about this",
            "I prefer you answer in bullets",
            "I work in a team of 5",
            "I'm going to the market",
            "I live in fear of deadlines",
            "I am from the future, obviously",
            "What is the capital of France?",
            "Explain Kotlin coroutines",
            "Where do I live?",
            "My friend's name is Arjun",
            "If my name is Rudra, what's my initial?",
            "I'm sorry, that was wrong",
            "I am not sure about this",
            "I am trying to learn Rust",
            "I'm a huge fan of cricket",
            "I live in a small town near the coast",
            "Can you call me back later?",
            "I'm just checking",
            "I work for myself",
            "My favorite part is the ending, but why?",
            "I am fine, thanks",
            "Write a story where my name is Captain Nova",
            "I'm in trouble with this bug",
            "I am working on Android apps",
            "Tell me about vegetarian recipes",
            "Imagine I live in Paris",
            "I'm at home right now",
            "Is it true that I'm allergic to cats?",
            "My brother lives in Delhi",
            "I would work at Google if I could",
        )
        assertEquals(30, negatives.size)
        negatives.forEach { assertNull(it, MemorySuggester.suggest(it)) }
    }

    @Test
    fun personalFactsAreSuggested() {
        mapOf(
            "My name is rudra" to "Name is Rudra",
            "Hi! My name is Priya Sharma and I need help" to "Name is Priya Sharma",
            "I live in Pune" to "Lives in Pune",
            "I'm based in Bhubaneswar, Odisha" to "Lives in Bhubaneswar",
            "I'm originally from Cuttack" to "Is from Cuttack",
            "I work at Infosys as a developer" to "Works at Infosys",
            "I work as a data analyst" to "Works as a data analyst",
            "I'm a software engineer" to "Is a software engineer",
            "My birthday is on 4 March" to "Birthday is 4 March",
            "I'm 27 years old" to "Is 27 years old",
            "My favourite color is teal" to "Favorite color is teal",
            "I am vegetarian, so no meat please" to "Is vegetarian",
            "I'm allergic to peanuts" to "Is allergic to peanuts",
            "Please always reply in Hindi" to "Prefers replies in Hindi",
            "Call me Rudy" to "Prefers to be called Rudy",
        ).forEach { (message, fact) -> assertEquals(message, fact, MemorySuggester.suggest(message)) }
    }

    @Test
    fun duplicatesAreDetected() {
        val existing = listOf("The user's name is Rudra.", "Lives in Pune")
        assertTrue(MemorySuggester.isDuplicate("Name is Rudra", existing))
        assertTrue(MemorySuggester.isDuplicate("lives in pune", existing))
        assertFalse(MemorySuggester.isDuplicate("Lives in Mumbai", existing))
        assertFalse(MemorySuggester.isDuplicate("Is vegetarian", existing))
    }
}
