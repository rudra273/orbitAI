package com.example.orbitai.core.model

import java.util.Locale

/** Decimal units, matching model providers' download sizes. */
fun formatModelSize(sizeBytes: Long): String {
    require(sizeBytes >= 0)
    return if (sizeBytes >= 1_000_000_000L) {
        String.format(Locale.US, "%.2f GB", sizeBytes / 1_000_000_000.0)
    } else {
        String.format(Locale.US, "%.1f MB", sizeBytes / 1_000_000.0)
    }
}
