package com.example.orbitai.core.engine

enum class RunOutcome { OK, FAILED, BROKEN_OUTPUT, TIMED_OUT, CRASHED_BEFORE }

/** One backend's speed-test run. Times are milliseconds; speed is streamed tokens per second. */
data class BackendRun(
    val backend: HardwareBackend,
    val outcome: RunOutcome,
    val loadMs: Long = 0,
    val firstTokenMs: Long = 0,
    val tokensPerSecond: Double = 0.0,
) {
    val ok get() = outcome == RunOutcome.OK

    /** Seconds for a typical reply: the first word, then [TYPICAL_REPLY_TOKENS] more. Lower is better. */
    val replySeconds: Double
        get() = firstTokenMs / 1000.0 + if (tokensPerSecond > 0) TYPICAL_REPLY_TOKENS / tokensPerSecond else Double.MAX_VALUE

    companion object {
        const val TYPICAL_REPLY_TOKENS = 150
    }
}

data class SpeedTestResults(val runs: List<BackendRun>) {

    /** Fastest typical reply; within 10% of it, the one that loads faster wins. */
    val winner: HardwareBackend?
        get() {
            val ok = runs.filter { it.ok }
            val best = ok.minOfOrNull { it.replySeconds } ?: return null
            return ok.filter { it.replySeconds <= best * 1.10 }.minBy { it.loadMs }.backend
        }

    /** True when [backend] ran but was no faster than the CPU, e.g. a GPU that quietly runs on the CPU. */
    fun noGainOverCpu(backend: HardwareBackend): Boolean {
        if (backend == HardwareBackend.CPU) return false
        val run = runs.firstOrNull { it.backend == backend && it.ok } ?: return false
        val cpu = runs.firstOrNull { it.backend == HardwareBackend.CPU && it.ok } ?: return false
        return run.replySeconds >= cpu.replySeconds * 0.90
    }

    fun encode(): String = runs.joinToString(";") {
        "${it.backend.name},${it.outcome.name},${it.loadMs},${it.firstTokenMs},${it.tokensPerSecond}"
    }

    companion object {
        fun decode(text: String): SpeedTestResults? = runCatching {
            SpeedTestResults(text.split(";").filter { it.isNotBlank() }.map { part ->
                val f = part.split(",")
                BackendRun(HardwareBackend.valueOf(f[0]), RunOutcome.valueOf(f[1]), f[2].toLong(), f[3].toLong(), f[4].toDouble())
            })
        }.getOrNull()

        /** Empty text, mostly the same word over and over, or mostly unreadable characters. */
        fun looksBroken(text: String): Boolean {
            if (text.isBlank()) return true
            if (text.count { it == '�' } * 10 > text.length) return true
            val words = text.lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
            if (words.size < 12) return false
            val mostCommon = words.groupingBy { it }.eachCount().maxOf { it.value }
            return mostCommon * 2 > words.size
        }
    }
}
