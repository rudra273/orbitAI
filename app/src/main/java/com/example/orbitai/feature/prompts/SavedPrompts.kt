package com.example.orbitai.feature.prompts

import android.content.Context
import com.example.orbitai.feature.automation.OrbitCommand
import com.example.orbitai.feature.automation.OrbitCommands
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** A reusable prompt. `{{input}}` in [text] marks where the cursor goes when it's inserted. */
data class SavedPrompt(val id: String = UUID.randomUUID().toString(), val title: String, val text: String)

/** Saved prompts, stored as JSON in SharedPreferences. */
class SavedPromptStore(context: Context) {

    private val prefs = context.getSharedPreferences("orbitai_saved_prompts", Context.MODE_PRIVATE)
    private val _prompts = MutableStateFlow(load())
    val prompts: StateFlow<List<SavedPrompt>> = _prompts.asStateFlow()

    fun save(prompt: SavedPrompt) = update { list ->
        if (list.any { it.id == prompt.id }) list.map { if (it.id == prompt.id) prompt else it } else list + prompt
    }

    fun delete(id: String) = update { list -> list.filterNot { it.id == id } }

    /** Moves a prompt one place up (-1) or down (+1). */
    fun move(id: String, delta: Int) = update { list ->
        val from = list.indexOfFirst { it.id == id }
        val to = from + delta
        if (from < 0 || to !in list.indices) list else list.toMutableList().apply { add(to, removeAt(from)) }
    }

    private fun update(transform: (List<SavedPrompt>) -> List<SavedPrompt>) {
        val next = transform(_prompts.value)
        _prompts.value = next
        prefs.edit().putString(KEY, JSONArray(next.map {
            JSONObject().put("id", it.id).put("title", it.title).put("text", it.text)
        }).toString()).apply()
    }

    private fun load(): List<SavedPrompt> = runCatching {
        val array = JSONArray(prefs.getString(KEY, null) ?: return emptyList())
        (0 until array.length()).map { i ->
            array.getJSONObject(i).let { SavedPrompt(it.getString("id"), it.getString("title"), it.getString("text")) }
        }
    }.getOrDefault(emptyList())

    private companion object {
        const val KEY = "prompts"
    }
}

/** An entry in the "/" menu. */
sealed interface PaletteItem {
    data class Command(val command: OrbitCommand) : PaletteItem
    data class Prompt(val prompt: SavedPrompt) : PaletteItem
}

/** Text to put in the input box and where the cursor should land. */
data class Insertion(val text: String, val cursor: Int)

object CommandPalette {

    const val PLACEHOLDER = "{{input}}"

    /** Shows the menu only while the input is a lone "/word" (no space yet). */
    fun query(input: String): String? =
        input.takeIf { it.startsWith("/") && it.none(Char::isWhitespace) }?.drop(1)

    fun items(query: String, prompts: List<SavedPrompt>): List<PaletteItem> {
        val q = query.lowercase()
        val commands = OrbitCommands.all
            .filter { cmd -> (listOf(cmd.name) + cmd.aliases).any { it.startsWith(q) } || cmd.title.lowercase().contains(q) }
            .map(PaletteItem::Command)
        val saved = prompts
            .filter { it.title.lowercase().contains(q) || it.text.lowercase().contains(q) }
            .map(PaletteItem::Prompt)
        return commands + saved
    }

    fun insertion(item: PaletteItem): Insertion = when (item) {
        is PaletteItem.Command -> "/${item.command.name} ".let { Insertion(it, it.length) }
        is PaletteItem.Prompt -> {
            val at = item.prompt.text.indexOf(PLACEHOLDER)
            if (at < 0) Insertion(item.prompt.text, item.prompt.text.length)
            else Insertion(item.prompt.text.replaceFirst(PLACEHOLDER, ""), at)
        }
    }
}
