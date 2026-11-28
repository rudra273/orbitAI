package com.example.orbitai.core.engine

import com.google.ai.edge.litertlm.Backend
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiteRtVisionBackendTest {
    @Test fun textOnlyModelsGetNoVisionBackend() {
        HardwareBackend.entries.forEach { assertNull(LiteRtLmEngine.visionBackendFor(supportsVision = false, backend = it)) }
    }

    @Test fun visionModelsRunTheEncoderOnTheCpuOrGpu() {
        assertTrue(LiteRtLmEngine.visionBackendFor(true, HardwareBackend.CPU) is Backend.CPU)
        assertTrue(LiteRtLmEngine.visionBackendFor(true, HardwareBackend.GPU) is Backend.GPU)
        assertTrue(LiteRtLmEngine.visionBackendFor(true, HardwareBackend.NPU) is Backend.GPU)
    }
}
