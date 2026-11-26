package com.example.orbitai.feature.spaces

/** Splits extracted text blocks (pages, paragraphs) into overlapping word windows for indexing. */
object DocumentChunker {

    const val CHUNK_WORDS = 500
    const val OVERLAP_WORDS = 125

    fun chunk(
        textBlocks: List<String>,
        chunkWordSize: Int = CHUNK_WORDS,
        overlapWordCount: Int = OVERLAP_WORDS,
    ): List<String> {
        if (textBlocks.isEmpty()) return emptyList()

        val chunks = mutableListOf<String>()
        var currentWords = mutableListOf<String>()

        fun flushChunk() {
            if (currentWords.isEmpty()) return
            chunks += currentWords.joinToString(" ")
            currentWords = currentWords.takeLast(overlapWordCount).toMutableList()
        }

        textBlocks.forEach { block ->
            val blockWords = block.split(Regex("\\s+")).filter { it.isNotBlank() }
            if (blockWords.isEmpty()) return@forEach

            if (currentWords.isNotEmpty() && currentWords.size + blockWords.size > chunkWordSize) {
                flushChunk()
            }

            if (blockWords.size >= chunkWordSize) {
                var start = 0
                while (start < blockWords.size) {
                    val preservedOverlap = if (currentWords.isEmpty()) emptyList() else currentWords
                    val availableWords = (chunkWordSize - preservedOverlap.size).coerceAtLeast(1)
                    val end = minOf(start + availableWords, blockWords.size)
                    currentWords = (preservedOverlap + blockWords.subList(start, end)).toMutableList()
                    flushChunk()
                    start = end
                }
            } else {
                currentWords.addAll(blockWords)
            }
        }

        if (currentWords.isNotEmpty()) chunks += currentWords.joinToString(" ")
        return chunks
    }

    fun normalize(text: String): String =
        text
            .replace(Regex("[\\t\\x0B\\f\\r ]+"), " ")
            .replace(Regex(" *\\n *"), "\n")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
}
