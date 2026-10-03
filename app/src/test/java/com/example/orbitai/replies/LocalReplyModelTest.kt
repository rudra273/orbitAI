package com.example.orbitai.replies

import com.example.orbitai.core.model.LlmModel
import com.example.orbitai.core.model.ModelProvider
import com.example.orbitai.feature.automation.replies.selectLocalReplyModel
import org.junit.Assert.*
import org.junit.Test

class LocalReplyModelTest {
    private val cloud = LlmModel("cloud", "Gemini", "", "", "", provider = ModelProvider.GEMINI)
    private val local = LlmModel("local", "Local", "model.task", "", "")

    @Test fun refusesCloudEvenWhenItIsTheUsersPreferredModel() {
        assertNull(selectLocalReplyModel(listOf(cloud), "cloud"))
        assertEquals(local, selectLocalReplyModel(listOf(cloud, local), "cloud"))
    }

    @Test fun keepsPreferredDownloadedLocalModel() {
        val second = local.copy(id = "second")
        assertEquals(second, selectLocalReplyModel(listOf(cloud, local, second), "second"))
        assertNull(selectLocalReplyModel(emptyList(), "local"))
    }
}
