package com.example.orbitai.feature.automation.device

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import androidx.core.net.toUri
import android.provider.AlarmClock
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.example.orbitai.feature.automation.executor.ContactResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Runs a confirmed [DeviceAction] by handing it to the right app. Nothing is sent or called
 * from here: SMS opens the messaging app with the text ready, and the user presses Send.
 */
class DeviceActionExecutor(context: Context) {

    private val app = context.applicationContext

    /** Returns a short line for the chat ("Timer set for 10 minutes.") or throws with a reason. */
    suspend fun execute(action: DeviceAction): String = when (action) {
        is DeviceAction.SetTimer -> {
            start(Intent(AlarmClock.ACTION_SET_TIMER)
                .putExtra(AlarmClock.EXTRA_LENGTH, action.seconds)
                .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                .apply { action.label?.let { putExtra(AlarmClock.EXTRA_MESSAGE, it) } }, "No clock app can set timers.")
            "Timer set for ${DeviceAction.duration(action.seconds)}."
        }
        is DeviceAction.SetAlarm -> {
            start(Intent(AlarmClock.ACTION_SET_ALARM)
                .putExtra(AlarmClock.EXTRA_HOUR, action.hour)
                .putExtra(AlarmClock.EXTRA_MINUTES, action.minute)
                .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                .apply { action.label?.let { putExtra(AlarmClock.EXTRA_MESSAGE, it) } }, "No clock app can set alarms.")
            "Alarm set for ${DeviceAction.clock(action.hour, action.minute)}."
        }
        is DeviceAction.SmsDraft -> {
            val number = phoneNumber(action.recipient)
            start(Intent(Intent.ACTION_SENDTO, ("smsto:" + Uri.encode(number.orEmpty())).toUri())
                .putExtra("sms_body", action.message), "No SMS app found.")
            if (number == null) "Opened your SMS app with the message. Choose ${action.recipient} there and press Send."
            else "Opened your SMS app with the message to ${action.recipient}. Press Send there."
        }
        is DeviceAction.Navigate -> {
            start(Intent(Intent.ACTION_VIEW, ("geo:0,0?q=" + Uri.encode(action.destination)).toUri()), "No maps app found.")
            "Opening directions to ${action.destination}."
        }
        is DeviceAction.OpenApp -> {
            val (label, intent) = launchable(action.appName) ?: error("Couldn't find an app called ${action.appName}.")
            start(intent, "Couldn't open $label.")
            "Opening $label."
        }
        is DeviceAction.WebSearch -> {
            val search = Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, action.query)
            try {
                start(search, "")
            } catch (_: IllegalStateException) {
                start(Intent(Intent.ACTION_VIEW, ("https://www.google.com/search?q=" + Uri.encode(action.query)).toUri()), "No browser found.")
            }
            "Searching the web for “${action.query}”."
        }
        is DeviceAction.CalendarEvent -> {
            start(Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, action.title)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, action.startMillis)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, action.endMillis), "No calendar app found.")
            "Opened your calendar with “${action.title}”. Save it there."
        }
        is DeviceAction.Flashlight -> {
            val camera = app.getSystemService(CameraManager::class.java)
            val id = camera.cameraIdList.firstOrNull {
                camera.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: error("This phone has no flashlight.")
            camera.setTorchMode(id, action.on)
            if (action.on) "Flashlight on." else "Flashlight off."
        }
    }

    private suspend fun start(intent: Intent, failure: String) = withContext(Dispatchers.Main) {
        try {
            app.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            error(failure)
        }
    }

    /** A number typed by the user, or the contact's first number when contacts access is granted. */
    private suspend fun phoneNumber(recipient: String): String? {
        val digits = recipient.filter { it.isDigit() || it == '+' }
        if (digits.count(Char::isDigit) >= 5 && recipient.none(Char::isLetter)) return digits
        if (ContextCompat.checkSelfPermission(app, android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return null
        return withContext(Dispatchers.IO) { runCatching { ContactResolver(app).findCallContacts(recipient).firstOrNull()?.number }.getOrNull() }
    }

    private fun launchable(name: String): Pair<String, Intent>? {
        val pm = app.packageManager
        val apps = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .filter { it.activityInfo.packageName != app.packageName }
        val labels = apps.map { it.loadLabel(pm).toString() }
        val index = AppMatcher.best(name, labels) ?: return null
        val info = apps[index].activityInfo
        return labels[index] to Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setClassName(info.packageName, info.name)
    }
}
