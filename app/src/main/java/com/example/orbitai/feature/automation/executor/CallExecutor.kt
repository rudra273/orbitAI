package com.example.orbitai.feature.automation.executor

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat

/** Accept phone digits and formatting, never dial strings containing service codes. */
fun callableNumber(value: String): String? {
    if (!Regex("\\+?[0-9() .-]+").matches(value.trim())) return null
    val normalized = value.trim().filter { it.isDigit() || it == '+' }
    return normalized.takeIf { it.count(Char::isDigit) in 3..15 }
}

data class CallContact(val name: String, val number: String)

class CallExecutor(private val context: Context) {
    fun launch(contact: CallContact, dialOnly: Boolean = false): String? {
        val number = callableNumber(contact.number) ?: return "That phone number isn't valid."
        val direct = !dialOnly && number.count(Char::isDigit) >= 7
        if (direct && ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return "Phone permission is required. You can open the dialer instead."
        }
        return try {
            context.startActivity(Intent(if (direct) Intent.ACTION_CALL else Intent.ACTION_DIAL,
                Uri.fromParts("tel", number, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            null
        } catch (_: android.content.ActivityNotFoundException) {
            "No phone app is available on this device."
        } catch (_: SecurityException) {
            "Android blocked the call. Try opening the dialer instead."
        }
    }
}
