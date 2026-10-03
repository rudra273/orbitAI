package com.example.orbitai.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceFitTest {

    // ActivityManager reports less than the marketed RAM.
    private val ram6 = 5_600_000_000L
    private val ram8 = 7_500_000_000L
    private val ram12 = 11_300_000_000L

    private fun fit(id: String, ram: Long) = DeviceFit.fitOf(AVAILABLE_MODELS.first { it.id == id }, ram)

    @Test fun recommendationScalesWithRam() {
        assertEquals("gemma3-1b-litertlm", DeviceFit.recommendedModelId(ram6))
        assertEquals("gemma4-e2b", DeviceFit.recommendedModelId(ram8))
        assertEquals("gemma4-e4b", DeviceFit.recommendedModelId(ram12))
    }

    @Test fun recommendedModelsExistInCatalog() {
        listOf(ram6, ram8, ram12).forEach { ram ->
            val id = DeviceFit.recommendedModelId(ram)!!
            assertTrue(AVAILABLE_MODELS.any { it.id == id })
        }
    }

    @Test fun hugeModelsAreTooLargeOnPhones() {
        assertEquals(ModelFit.TOO_LARGE, fit("onnx-phi4", ram12))
        assertEquals(ModelFit.TOO_LARGE, fit("onnx-gemma3-4b-it", ram6))
    }

    @Test fun smallModelsFitEverywhere() {
        listOf(ram6, ram8, ram12).forEach { assertEquals(ModelFit.GOOD, fit("gemma3-1b-litertlm", it)) }
    }

    @Test fun midModelsMayBeSlowOnSmallerPhones() {
        assertEquals(ModelFit.SLOW, fit("gemma4-e4b", ram8))
        assertEquals(ModelFit.GOOD, fit("gemma4-e4b", ram12))
    }

    @Test fun unknownRamOrSizeIsNotBlocked() {
        assertEquals(ModelFit.GOOD, DeviceFit.classify(null, ram6))
        assertEquals(ModelFit.GOOD, DeviceFit.classify(5_000_000_000L, 0L))
    }

    @Test fun storageNeedsTenPercentMargin() {
        assertTrue(DeviceFit.hasStorageFor(1_000_000_000L, 1_100_000_000L))
        assertFalse(DeviceFit.hasStorageFor(1_000_000_000L, 1_050_000_000L))
    }
}
