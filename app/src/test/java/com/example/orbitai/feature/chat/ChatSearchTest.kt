package com.example.orbitai.feature.chat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatSearchTest {

    @Test
    fun wordsBecomePrefixTerms() {
        assertEquals("pine*", ChatSearch.matchQuery("pine"))
        assertEquals("router* err* 4012*", ChatSearch.matchQuery("Router ERR-4012"))
        assertEquals("नमस्ते*", ChatSearch.matchQuery("नमस्ते"))
    }

    @Test
    fun userSyntaxCannotBreakTheQuery() {
        assertEquals("a* b*", ChatSearch.matchQuery("\"a\" OR b*"))
        assertEquals("cats* dogs*", ChatSearch.matchQuery("cats NOT dogs NEAR"))
        assertNull(ChatSearch.matchQuery("  \"*()-  "))
        assertNull(ChatSearch.matchQuery(""))
    }

    @Test
    fun longQueriesAreCapped() {
        assertEquals(8, ChatSearch.matchQuery((1..20).joinToString(" ") { "w$it" })!!.split(" ").size)
    }
}
