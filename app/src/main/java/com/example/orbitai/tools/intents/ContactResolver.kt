package com.example.orbitai.tools.intents

import android.content.Context
import android.provider.ContactsContract

class ContactResolver(context: Context) {

    private val contentResolver = context.applicationContext.contentResolver

    fun findPhoneNumberByName(name: String): String? {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return null

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$trimmedName%")

        contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null,
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)

            var partialMatch: String? = null
            while (cursor.moveToNext()) {
                val displayName = cursor.getString(nameIndex).orEmpty()
                val phoneNumber = cursor.getString(numberIndex).orEmpty()
                if (phoneNumber.isBlank()) continue

                if (displayName.equals(trimmedName, ignoreCase = true)) {
                    return normalizePhoneNumber(phoneNumber)
                }

                if (partialMatch == null) {
                    partialMatch = normalizePhoneNumber(phoneNumber)
                }
            }

            return partialMatch
        }

        return null
    }

    private fun normalizePhoneNumber(value: String): String {
        return buildString {
            value.forEachIndexed { index, char ->
                when {
                    char.isDigit() -> append(char)
                    char == '+' && index == 0 -> append(char)
                }
            }
        }
    }
}