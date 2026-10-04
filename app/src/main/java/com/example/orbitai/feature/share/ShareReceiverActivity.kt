package com.example.orbitai.feature.share

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.IntentCompat
import androidx.lifecycle.lifecycleScope
import com.example.orbitai.MainActivity
import com.example.orbitai.appContainer
import com.example.orbitai.feature.bubble.ProcessTextActivity
import kotlinx.coroutines.launch

/**
 * Receives PDFs, images and text files from the share sheet: "Ask Orbit about this" opens a
 * new chat with them attached, "Add to Space…" indexes the documents in a Space.
 * Shared plain text (no file) goes to [ProcessTextActivity] as before.
 */
class ShareReceiverActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) return

        val uris = sharedUris(intent)
        if (uris.isEmpty()) {
            val text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
            if (!text.isNullOrBlank()) {
                startActivity(Intent(this, ProcessTextActivity::class.java).setAction(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text))
            }
            finish()
            return
        }

        lifecycleScope.launch {
            val imported = SharedFileImporter(applicationContext).import(uris)
            if (imported.skipped.isNotEmpty()) {
                Toast.makeText(this@ShareReceiverActivity, "Orbit can't open: ${imported.skipped.joinToString()}", Toast.LENGTH_LONG).show()
            }
            val files = imported.files
            when {
                files.isEmpty() -> finish()
                files.none { it.isDocument } -> askOrbit(files)
                else -> chooseAction(files)
            }
        }
    }

    private fun sharedUris(intent: Intent): List<Uri> = when (intent.action) {
        Intent.ACTION_SEND -> listOfNotNull(IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java))
        Intent.ACTION_SEND_MULTIPLE -> IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
        else -> emptyList()
    }

    private fun chooseAction(files: List<SharedFile>) {
        val label = if (files.size == 1) files.single().name else "${files.size} files"
        AlertDialog.Builder(this)
            .setTitle(label)
            .setItems(arrayOf("Ask Orbit about this", "Add to Space…")) { _, which ->
                if (which == 0) askOrbit(files) else pickSpace(files)
            }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun askOrbit(files: List<SharedFile>) {
        startActivity(Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putStringArrayListExtra(MainActivity.EXTRA_SHARED_FILES, ArrayList(files.map { it.uri.toString() }))
        })
        finish()
    }

    private fun pickSpace(files: List<SharedFile>) {
        val repo = appContainer.spaceRepository
        lifecycleScope.launch {
            val spaces = repo.currentSpaces()
            AlertDialog.Builder(this@ShareReceiverActivity)
                .setTitle("Add to Space")
                .setItems((spaces.map { it.name } + "New Space…").toTypedArray()) { _, which ->
                    if (which < spaces.size) addToSpace(files, spaces[which].id, spaces[which].name)
                    else newSpace(files)
                }
                .setOnCancelListener { finish() }
                .show()
        }
    }

    private fun newSpace(files: List<SharedFile>) {
        val input = EditText(this).apply { hint = "Space name"; setSingleLine() }
        val container = FrameLayout(this).apply {
            val pad = (20 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 2, pad, 0)
            addView(input)
        }
        AlertDialog.Builder(this)
            .setTitle("New Space")
            .setView(container)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim().ifBlank { "Shared files" }
                lifecycleScope.launch {
                    val space = appContainer.spaceRepository.createSpace(name)
                    addToSpace(files, space.id, space.name)
                }
            }
            .setNegativeButton("Cancel") { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun addToSpace(files: List<SharedFile>, spaceId: String, spaceName: String) {
        val documents = files.filter { it.isDocument }
        documents.forEach { appContainer.spaceRepository.addDocumentToSpace(it.uri, spaceId) }
        val skippedImages = files.size - documents.size
        val message = buildString {
            append("Adding ${documents.size} ${if (documents.size == 1) "file" else "files"} to $spaceName")
            if (skippedImages > 0) append(". Spaces can't index images, so $skippedImages skipped")
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }
}
