package com.example.orbitai.data

import java.util.Locale

val SUPPORTED_DOCUMENT_MIME_TYPES = arrayOf(
    "application/pdf",
    "text/plain",
    "text/markdown",
    "text/x-markdown",
    "text/csv",
    "application/json",
    "application/xml",
    "text/xml",
    "text/html",
)

private val extensionToMimeType = mapOf(
    "pdf" to "application/pdf",
    "txt" to "text/plain",
    "md" to "text/markdown",
    "markdown" to "text/markdown",
    "csv" to "text/csv",
    "json" to "application/json",
    "xml" to "application/xml",
    "html" to "text/html",
    "htm" to "text/html",
)

fun normalizeDocumentMimeType(rawMimeType: String?, fileName: String): String {
    val normalizedMimeType = rawMimeType?.substringBefore(';')?.trim()?.lowercase(Locale.US)
    if (!normalizedMimeType.isNullOrEmpty() && normalizedMimeType != "application/octet-stream") {
        return normalizedMimeType
    }

    val extension = fileName.substringAfterLast('.', "").lowercase(Locale.US)
    return extensionToMimeType[extension] ?: "application/octet-stream"
}

fun isTextLikeDocument(mimeType: String): Boolean =
    mimeType.startsWith("text/") ||
        mimeType == "application/json" ||
        mimeType == "application/xml"
