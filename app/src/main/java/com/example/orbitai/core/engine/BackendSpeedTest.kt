package com.example.orbitai.core.engine

import android.content.Context
import android.os.SystemClock
import com.example.orbitai.core.common.InferenceSettings
import com.example.orbitai.core.model.LlmModel
import com.example.orbitai.core.prompt.ModelPromptBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.coroutines.cancellation.CancellationException

/**
 * Loads a model on each backend it supports, reads a fixed prompt and writes a short reply,
 * then saves the timings so Auto uses the fastest backend for this phone.
 */
class BackendSpeedTest(private val context: Context, private val repo: LlmRepository) {

    /** Runs the test; [onBackend] is called as each backend starts. Throws if a reply is being written. */
    suspend fun run(model: LlmModel, onBackend: (HardwareBackend) -> Unit): SpeedTestResults = withContext(Dispatchers.IO) {
        check(repo.releaseIfIdle()) { "Wait for the current reply to finish, then test again." }
        val cascade = BackendCascade(context, File(model.fileName).name)
        val crashed = cascade.prepareForTest()
        try {
            val runs = supportedBackends(model).map { backend ->
                onBackend(backend)
                if (backend in crashed) BackendRun(backend, RunOutcome.CRASHED_BEFORE) else measure(model, backend, cascade)
            }
            SpeedTestResults(runs).also { BackendSettingsStore(context).saveResults(model.id, it) }
        } finally {
            repo.releaseIfIdle()
        }
    }

    private suspend fun measure(model: LlmModel, backend: HardwareBackend, cascade: BackendCascade): BackendRun {
        repo.releaseIfIdle()
        val loadStart = SystemClock.elapsedRealtime()
        try {
            repo.loadModel(model, SETTINGS, forced = backend)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return BackendRun(backend, RunOutcome.FAILED)
        }
        val loadMs = SystemClock.elapsedRealtime() - loadStart

        val prompt = ModelPromptBuilder.wrapInstructionPrompt(model.promptStyle, TEST_INSTRUCTION)
        val text = StringBuilder()
        var chunks = 0
        var firstAt = 0L
        var lastAt = 0L
        repo.beginUse()
        val start = SystemClock.elapsedRealtime()
        try {
            withTimeout(TIMEOUT_MS) {
                repo.generateResponseStream(InferenceInput(prompt), SETTINGS.maxDecodedTokens).collect { part ->
                    val now = SystemClock.elapsedRealtime()
                    if (chunks == 0) firstAt = now
                    lastAt = now
                    chunks++
                    text.append(part)
                }
            }
        } catch (_: TimeoutCancellationException) {
            cascade.markFailed(backend)
            return BackendRun(backend, RunOutcome.TIMED_OUT, loadMs)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            cascade.markFailed(backend)
            return BackendRun(backend, RunOutcome.FAILED, loadMs)
        } finally {
            repo.endUse()
        }

        if (SpeedTestResults.looksBroken(text.toString())) {
            cascade.markFailed(backend)
            return BackendRun(backend, RunOutcome.BROKEN_OUTPUT, loadMs)
        }
        val writingMs = lastAt - firstAt
        val tokensPerSecond = if (chunks > 1 && writingMs > 0) (chunks - 1) * 1000.0 / writingMs else 0.0
        return BackendRun(backend, RunOutcome.OK, loadMs, firstAt - start, tokensPerSecond)
    }

    private companion object {
        val SETTINGS = InferenceSettings(maxDecodedTokens = 64)
        const val TIMEOUT_MS = 90_000L

        // About 300 tokens of plain text, the same for every backend.
        const val TEST_INSTRUCTION = "Read the note below, then explain in a few sentences how a small town could cut its water use.\n\n" +
            "Note: Riverton is a town of about twelve thousand people. Its water comes from one river and two wells. " +
            "Over the last ten summers the river has run lower each year, and in the hottest weeks the town has asked people " +
            "to stop watering lawns. Most of the water goes to homes, but the school, the hospital, a food factory and three parks " +
            "use a large share. Many houses are more than forty years old and still have old toilets, taps and pipes. The town " +
            "found leaks in several streets last year but fixed only some of them. Farmers outside the town draw from the same " +
            "river for their fields. The council has a small budget, a volunteer group that wants to help, and a local paper that " +
            "most families read. Some people collect rain in barrels, but there are no rules or rewards for it. Water bills are the " +
            "same price no matter how much a home uses, and most people do not know how much water they use each month."
    }
}
