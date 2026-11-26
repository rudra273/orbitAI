package com.example.orbitai.feature.spaces

import java.io.FileNotFoundException

/** A document problem with a message meant for the user. */
class IngestException(message: String) : Exception(message)

/** Turns an indexing failure into a short reason shown on the document. */
object IngestFailure {

    const val SCANNED_PDF = "No selectable text (scanned PDF). Orbit can't read text inside images yet."
    const val EMPTY_FILE = "The file has no text to index."
    const val UNSUPPORTED = "This file type can't be indexed. Use PDF, Word (.docx), e-book (.epub) or a text file."

    fun reasonFor(error: Throwable): String = when {
        error is IngestException -> error.message.orEmpty()
        error is OutOfMemoryError -> "Too large to process on this phone. Try splitting the file."
        error is FileNotFoundException || error is SecurityException ->
            "File no longer accessible. Remove it and add it again."
        error.javaClass.simpleName == "InvalidPasswordException" ||
            error.message.orEmpty().contains("password", ignoreCase = true) -> "Password-protected PDF. Remove the password and add it again."
        error.javaClass.simpleName.contains("Parse", ignoreCase = true) ||
            error.message.orEmpty().contains("header", ignoreCase = true) -> "The file looks damaged and couldn't be read."
        else -> "Couldn't read this file (${error.javaClass.simpleName})."
    }
}
