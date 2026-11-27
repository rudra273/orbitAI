package com.example.orbitai.feature.spaces.embedding

import org.junit.Assert.assertEquals
import org.junit.Test

class EmbeddingGemmaPromptTest {
    @Test fun queriesUseTheSearchTaskPrefix() {
        assertEquals("task: search result | query: what is orbit", EmbeddingGemmaTextEmbedder.prompt("what is orbit", EmbedKind.QUERY))
    }

    @Test fun storedTextUsesTheUntitledDocumentFormat() {
        assertEquals("title: none | text: Orbit runs on the phone.", EmbeddingGemmaTextEmbedder.prompt("Orbit runs on the phone.", EmbedKind.PASSAGE))
    }
}
