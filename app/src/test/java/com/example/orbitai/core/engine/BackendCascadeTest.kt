package com.example.orbitai.core.engine

import com.example.orbitai.core.engine.HardwareBackend.CPU
import com.example.orbitai.core.engine.HardwareBackend.GPU
import com.example.orbitai.core.engine.HardwareBackend.NPU
import org.junit.Assert.assertEquals
import org.junit.Test

class BackendCascadeTest {

    @Test
    fun fastestFirstAndCpuAlwaysLast() {
        assertEquals(listOf(NPU, GPU, CPU), BackendCascade.Planner.order(listOf(CPU, GPU, NPU), emptySet()))
        assertEquals(listOf(GPU, CPU), BackendCascade.Planner.order(listOf(GPU, CPU), emptySet()))
    }

    @Test
    fun failedBackendsAreSkipped() {
        assertEquals(listOf(GPU, CPU), BackendCascade.Planner.order(listOf(NPU, GPU, CPU), setOf(NPU)))
        assertEquals(listOf(CPU), BackendCascade.Planner.order(listOf(NPU, GPU, CPU), setOf(NPU, GPU)))
        // CPU is the last resort even if it was recorded as failed.
        assertEquals(listOf(CPU), BackendCascade.Planner.order(listOf(NPU, GPU, CPU), setOf(NPU, GPU, CPU)))
    }
}
