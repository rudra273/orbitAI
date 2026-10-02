package com.example.orbitai.core.model

import org.junit.Assert.*
import org.junit.Test

class ModelCatalogTest {
    @Test fun sizesUseDecimalDownloadUnits() {
        assertEquals("6.1 MB", formatModelSize(6_120_274))
        assertEquals("584.4 MB", formatModelSize(584_417_280))
        assertEquals("1.00 GB", formatModelSize(1_000_000_000))
        assertEquals("2.59 GB", formatModelSize(2_588_147_712))
        assertEquals("10.92 GB", formatModelSize(10_916_042_658))
    }

    @Test fun multipartDownloadSizesIncludeEveryFile() {
        val spec = ModelDownloadSpec(listOf(
            ModelDownloadFile("model.onnx", "https://example.org/model", 100),
            ModelDownloadFile("model.onnx.data", "https://example.org/weights", 2_000_000_000),
        ))
        assertEquals(2_000_000_100L, spec.totalSizeBytes)
        assertNull(spec.copy(files = spec.files + ModelDownloadFile("tokenizer", "https://example.org/tokenizer")).totalSizeBytes)
        assertNull(ModelDownloadSpec(emptyList()).totalSizeBytes)
    }

    @Test fun everyCatalogModelHasSizedPinnedDownloads() {
        assertEquals(AVAILABLE_MODELS.size, AVAILABLE_MODELS.map { it.id }.distinct().size)
        assertEquals(AVAILABLE_MODELS.map { it.id }.toSet(), MODEL_DOWNLOAD_SPECS.keys)
        AVAILABLE_MODELS.forEach { model ->
            val spec = MODEL_DOWNLOAD_SPECS.getValue(model.id)
            assertTrue("Missing size for ${model.id}", (spec.totalSizeBytes ?: 0) > 0)
            assertEquals(spec.files.size, spec.files.map { it.relativePath }.distinct().size)
            spec.files.forEach { file ->
                assertTrue(file.url.matches(Regex("https://huggingface.co/.+/resolve/[a-f0-9]{40}/.+\\?download=true")))
                assertTrue(file.sizeBytes!! > 0)
            }
            if (model.format != ModelFormat.ONNX_GENAI) {
                assertEquals(model.fileName, spec.files.single().relativePath)
            }
        }
        assertTrue(MODEL_DOWNLOAD_SPECS.getValue("gemma2-2b").files.single().url.contains("Gemma2-2B-IT_multi-prefill-seq_q8_ekv1280.task"))
    }
}
