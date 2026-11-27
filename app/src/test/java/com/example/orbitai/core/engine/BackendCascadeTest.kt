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

    @Test
    fun testedWinnerGoesFirst() {
        assertEquals(listOf(CPU, GPU), BackendCascade.Planner.order(listOf(GPU, CPU), emptySet(), tested = CPU))
        assertEquals(listOf(GPU, NPU, CPU), BackendCascade.Planner.order(listOf(NPU, GPU, CPU), emptySet(), tested = GPU))
        // A winner that has since failed is skipped.
        assertEquals(listOf(CPU), BackendCascade.Planner.order(listOf(GPU, CPU), setOf(GPU), tested = GPU))
    }

    @Test
    fun expectedIsWinnerOrFastestSupported() {
        assertEquals(GPU, BackendCascade.Planner.expected(listOf(GPU, CPU), null))
        assertEquals(CPU, BackendCascade.Planner.expected(listOf(GPU, CPU), CPU))
        assertEquals(GPU, BackendCascade.Planner.expected(listOf(GPU, CPU), NPU))
    }

    @Test
    fun fallbackLabelNamesTheFailedBackend() {
        assertEquals("CPU (GPU failed)", BackendLoad(Unit, CPU, GPU).label)
        assertEquals("GPU", BackendLoad(Unit, GPU, null).label)
    }
}
