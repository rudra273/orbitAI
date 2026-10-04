package com.example.orbitai.feature.spaces.embedding

import com.google.gson.stream.JsonReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.text.Normalizer

/**
 * SentencePiece Unigram tokenizer for XLM-R style models (multilingual-e5), read from a Hugging Face
 * `tokenizer.json`. Text is normalised (NFKC, spaces collapsed), marked with "▁" at word starts, split
 * into the highest-scoring pieces (Viterbi), and wrapped in `<s>` ... `</s>`.
 *
 * Differences from the reference: the model's compiled normalisation table is approximated by NFKC,
 * which can differ for a few rare characters (for example zero-width spaces).
 */
class UnigramTokenizer private constructor(
    private val ids: HashMap<String, Int>,
    private val scores: FloatArray,
    private val unkId: Int,
    private val maxPieceChars: Int,
) {

    val bosId = 0
    val padId = 1
    val eosId = 2

    /** Token ids including `<s>` and `</s>`, cut to [maxTokens] (the closing `</s>` is always kept). */
    fun encode(text: String, maxTokens: Int = 512): IntArray {
        val body = pieces(normalize(text))
        val limit = (maxTokens - 2).coerceAtLeast(0)
        val kept = if (body.size > limit) body.copyOf(limit) else body
        return IntArray(kept.size + 2).also {
            it[0] = bosId
            kept.copyInto(it, 1)
            it[it.size - 1] = eosId
        }
    }

    internal fun normalize(text: String): String {
        val folded = Normalizer.normalize(text, Normalizer.Form.NFKC)
        val sb = StringBuilder(folded.length)
        for (ch in folded) {
            when {
                ch == '\t' || ch == '\n' || ch == '\r' || ch == '\u000B' || ch == '\u000C' || ch == ' ' -> sb.append(' ')
                Character.getType(ch) == Character.CONTROL.toInt() -> Unit
                ch == '\u200C' || ch == '\u200D' || ch == '\uFEFF' -> Unit
                else -> sb.append(ch)
            }
        }
        val collapsed = sb.toString().replace(Regex(" {2,}"), " ")
        if (collapsed.isEmpty()) return ""
        val marked = collapsed.replace(' ', '▁')
        return if (marked.startsWith('▁')) marked else "▁$marked"
    }

    private fun pieces(normalized: String): IntArray {
        if (normalized.isEmpty()) return IntArray(0)
        val cps = normalized.codePoints().toArray()
        val n = cps.size
        val best = FloatArray(n + 1) { Float.NEGATIVE_INFINITY }
        val from = IntArray(n + 1)
        val pieceId = IntArray(n + 1)
        best[0] = 0f
        val unkScore = (scores.minOrNull() ?: 0f) - 10f
        val buffer = StringBuilder()
        for (start in 0 until n) {
            if (best[start] == Float.NEGATIVE_INFINITY) continue
            buffer.setLength(0)
            var end = start
            var matchedAny = false
            while (end < n && end - start < maxPieceChars) {
                buffer.appendCodePoint(cps[end])
                end++
                val id = ids[buffer.toString()] ?: continue
                matchedAny = true
                val candidate = best[start] + scores[id]
                if (candidate > best[end]) { best[end] = candidate; from[end] = start; pieceId[end] = id }
            }
            if (!matchedAny || best[start + 1] == Float.NEGATIVE_INFINITY) {
                val candidate = best[start] + unkScore
                if (candidate > best[start + 1]) { best[start + 1] = candidate; from[start + 1] = start; pieceId[start + 1] = unkId }
            }
        }
        val out = ArrayList<Int>()
        var i = n
        while (i > 0) { out.add(pieceId[i]); i = from[i] }
        out.reverse()
        return out.toIntArray()
    }

    companion object {
        fun load(file: File): UnigramTokenizer = file.inputStream().buffered().use(::load)

        fun load(input: InputStream): UnigramTokenizer {
            val ids = HashMap<String, Int>(300_000)
            val scoreList = ArrayList<Float>(260_000)
            var unk = 3
            var maxChars = 1
            JsonReader(InputStreamReader(input, Charsets.UTF_8)).use { reader ->
                reader.beginObject()
                while (reader.hasNext()) {
                    if (reader.nextName() != "model") { reader.skipValue(); continue }
                    reader.beginObject()
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "unk_id" -> unk = reader.nextInt()
                            "vocab" -> {
                                reader.beginArray()
                                while (reader.hasNext()) {
                                    reader.beginArray()
                                    val piece = reader.nextString()
                                    val score = reader.nextDouble().toFloat()
                                    reader.endArray()
                                    ids[piece] = scoreList.size
                                    scoreList.add(score)
                                    val chars = piece.codePointCount(0, piece.length)
                                    if (chars > maxChars) maxChars = chars
                                }
                                reader.endArray()
                            }
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()
                }
                reader.endObject()
            }
            // Special tokens (<s>, <pad>, </s>, <unk>) carry no text; they are never produced from user text.
            listOf("<s>", "<pad>", "</s>", "<unk>").forEach { ids.remove(it) }
            return UnigramTokenizer(ids, scoreList.toFloatArray(), unk, maxChars)
        }
    }
}
