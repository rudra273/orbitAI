package com.example.orbitai.feature.shortcuts

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.orbitai.MainActivity
import com.example.orbitai.R
import com.example.orbitai.feature.chat.ChatSummary

/** Long-press launcher shortcuts for the most recent chats. */
object RecentChatShortcuts {

    private const val PREFIX = "chat:"
    private const val COUNT = 2

    /** Recent non-archived chats that have messages, newest first. */
    fun pick(chats: List<ChatSummary>): List<ChatSummary> =
        chats.filter { !it.isArchived && it.lastMessageAt != null }
            .sortedByDescending { it.lastMessageAt }
            .take(COUNT)

    fun title(chat: ChatSummary): String =
        chat.title.trim().takeUnless { it.isBlank() || it.equals("New Chat", ignoreCase = true) }?.take(25) ?: "Recent chat"

    /** Replaces the dynamic shortcuts and disables pinned ones whose chat no longer exists. */
    fun update(context: Context, chats: List<ChatSummary>) {
        val recent = pick(chats)
        val shortcuts = recent.map { chat ->
            ShortcutInfoCompat.Builder(context, PREFIX + chat.id)
                .setShortLabel(title(chat))
                .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                .setIntent(
                    Intent(context, MainActivity::class.java)
                        .setAction(MainActivity.ACTION_OPEN_CHAT)
                        .putExtra(MainActivity.EXTRA_CHAT_ID, chat.id),
                )
                .build()
        }
        runCatching {
            ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
            val existing = chats.map { PREFIX + it.id }.toSet()
            val stale = ShortcutManagerCompat.getShortcuts(context, ShortcutManagerCompat.FLAG_MATCH_PINNED)
                .map { it.id }
                .filter { it.startsWith(PREFIX) && it !in existing }
            if (stale.isNotEmpty()) ShortcutManagerCompat.disableShortcuts(context, stale, "This chat was deleted")
        }
    }
}
