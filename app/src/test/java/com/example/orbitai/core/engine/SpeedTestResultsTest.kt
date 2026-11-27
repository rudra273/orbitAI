package com.example.orbitai.core.engine

import com.example.orbitai.core.engine.HardwareBackend.CPU
import com.example.orbitai.core.engine.HardwareBackend.GPU
import com.example.orbitai.core.engine.HardwareBackend.NPU
import com.example.orbitai.core.model.LlmModel
import com.example.orbitai.core.model.ModelFormat
import com.example.orbitai.core.model.ModelProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedTestResultsTest {

    private fun ok(backend: HardwareBackend, loadMs: Long, firstMs: Long, tps: Double) =
        BackendRun(backend, RunOutcome.OK, loadMs, firstMs, tps)

    @Test
    fun fastestReplyWins() {
        val results = SpeedTestResults(listOf(ok(GPU, 3000, 400, 30.0), ok(CPU, 1000, 2000, 12.0)))
        assertEquals(GPU, results.winner)
    }

    @Test
    fun nearTieGoesToFasterLoad() {
        // GPU 0.4 + 150/20 = 7.9 s, CPU 0.5 + 150/19 = 8.4 s: within 10%, CPU loads faster.
        val results = SpeedTestResults(listOf(ok(GPU, 5000, 400, 20.0), ok(CPU, 1500, 500, 19.0)))
        assertEquals(CPU, results.winner)
        assertTrue(results.noGainOverCpu(GPU))
    }

    @Test
    fun failedRunsNeverWin() {
        val results = SpeedTestResults(listOf(BackendRun(NPU, RunOutcome.FAILED), BackendRun(GPU, RunOutcome.BROKEN_OUTPUT), ok(CPU, 1000, 900, 10.0)))
        assertEquals(CPU, results.winner)
        assertNull(SpeedTestResults(listOf(BackendRun(GPU, RunOutcome.TIMED_OUT))).winner)
    }

    @Test
    fun encodeRoundTrips() {
        val results = SpeedTestResults(listOf(ok(GPU, 3000, 400, 30.5), BackendRun(CPU, RunOutcome.CRASHED_BEFORE)))
        assertEquals(results, SpeedTestResults.decode(results.encode()))
        assertNull(SpeedTestResults.decode("garbage"))
    }

    @Test
    fun brokenOutputIsDetected() {
        assertTrue(SpeedTestResults.looksBroken("   "))
        assertTrue(SpeedTestResults.looksBroken("the the the the the the the the the the the the the"))
        assertTrue(SpeedTestResults.looksBroken("���ab"))
        assertFalse(SpeedTestResults.looksBroken("The town could fix leaking pipes, price water by use, and reward rain barrels."))
    }

    @Test
    fun npuOnlyForModelsBuiltForThisChip() {
        val lite = LlmModel("m", "M", "m.litertlm", "", "1B", format = ModelFormat.LITERTLM, npuSocModels = setOf("SM8750"))
        assertEquals(listOf(NPU, GPU, CPU), supportedBackends(lite, socModel = "SM8750"))
        assertEquals(listOf(GPU, CPU), supportedBackends(lite, socModel = "SM8650"))
        assertEquals(listOf(GPU, CPU), supportedBackends(lite.copy(format = ModelFormat.TASK), socModel = "SM8750"))
        assertEquals(listOf(CPU), supportedBackends(lite.copy(format = ModelFormat.ONNX_GENAI), socModel = "SM8750"))
        assertEquals(emptyList<HardwareBackend>(), supportedBackends(lite.copy(provider = ModelProvider.GEMINI), socModel = "SM8750"))
    }
}
