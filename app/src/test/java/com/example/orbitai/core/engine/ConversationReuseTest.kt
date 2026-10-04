package com.example.orbitai.core.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationReuseTest {

    private val key = "chat-1|gemma|settings|system"

    @Test
    fun reusesWhenNothingChanged() {
        assertTrue(ConversationReuse.canReuse(key, listOf("u1", "a1"), 500, key, listOf("u1", "a1"), 4096))
    }

    @Test
    fun rebuildsAfterChatModelOrModeChange() {
        assertFalse(ConversationReuse.canReuse(key, listOf("u1", "a1"), 500, "chat-2|gemma|settings|system", listOf("u1", "a1"), 4096))
        assertFalse(ConversationReuse.canReuse(null, emptyList(), 0, key, emptyList(), 4096))
    }

    @Test
    fun rebuildsAfterEditRegenerateOrTrimming() {
        // Regenerate removed the old reply; edit changed the ids; trimming dropped the oldest turn.
        assertFalse(ConversationReuse.canReuse(key, listOf("u1", "a1", "u2", "a2"), 500, key, listOf("u1", "a1"), 4096))
        assertFalse(ConversationReuse.canReuse(key, listOf("u1", "a1"), 500, key, listOf("u1", "a1-new"), 4096))
        assertFalse(ConversationReuse.canReuse(key, listOf("u1", "a1", "u2", "a2"), 500, key, listOf("u2", "a2"), 4096))
    }

    @Test
    fun rebuildsWhenTheSessionIsNearlyFull() {
        assertFalse(ConversationReuse.canReuse(key, listOf("u1", "a1"), 3500, key, listOf("u1", "a1"), 4096))
    }
}
