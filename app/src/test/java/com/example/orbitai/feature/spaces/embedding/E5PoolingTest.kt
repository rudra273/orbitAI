package com.example.orbitai.feature.spaces.embedding

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.sqrt

class E5PoolingTest {

    @Test
    fun meanOfTokenVectorsIsScaledToLengthOne() {
        val pooled = E5TextEmbedder.meanPooled(arrayOf(floatArrayOf(2f, 0f), floatArrayOf(0f, 2f)))
        assertEquals(1f / sqrt(2f), pooled[0], 1e-6f)
        assertEquals(1f / sqrt(2f), pooled[1], 1e-6f)
    }

    @Test
    fun anAllZeroOutputStaysZeroInsteadOfBecomingNaN() {
        val pooled = E5TextEmbedder.meanPooled(arrayOf(floatArrayOf(0f, 0f)))
        assertEquals(0f, pooled[0], 0f)
    }
}
