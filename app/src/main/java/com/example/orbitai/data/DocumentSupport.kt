package com.example.orbitai.data

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
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
    "image/*",
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

fun isImageDocument(mimeType: String): Boolean = mimeType.startsWith("image/")

fun extractDocumentText(
    context: Context,
    uri: Uri,
    mimeType: String,
): String = when {
    mimeType == "application/pdf" -> extractPdfText(context, uri)
    isTextLikeDocument(mimeType) -> {
        context.contentResolver.openInputStream(uri)
            ?.bufferedReader()
            ?.use { it.readText() }
            .orEmpty()
            .replace("\r\n", "\n")
            .trim()
    }
    else -> ""
}

private fun extractPdfText(context: Context, uri: Uri): String = try {
    PDFBoxResourceLoader.init(context)
    context.contentResolver.openInputStream(uri)?.use { input ->
        PDDocument.load(input).use { document ->
            val stripper = PDFTextStripper().apply { sortByPosition = true }
            buildString {
                for (pageNumber in 1..document.numberOfPages) {
                    stripper.startPage = pageNumber
                    stripper.endPage = pageNumber
                    val pageText = stripper.getText(document).trim()
                    if (pageText.isNotBlank()) {
                        if (isNotEmpty()) append("\n\n")
                        append("[Page ").append(pageNumber).append("]\n").append(pageText)
                    }
                }
            }.trim()
        }
    }.orEmpty()
} catch (_: Exception) {
    ""
}
