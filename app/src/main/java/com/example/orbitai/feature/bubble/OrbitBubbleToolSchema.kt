package com.example.orbitai.feature.bubble

import com.google.ai.edge.litertlm.Tool
import com.google.ai.edge.litertlm.ToolParam
import com.google.ai.edge.litertlm.ToolSet

/**
 * Tool schemas specific to the floating bubble context.
 * These tools are only available when running in the bubble overlay.
 *
 * Passed to [LlmConversationEngine.createConversation] via the toolSchemas parameter.
 */
class OrbitBubbleToolSchema : ToolSet {

    @Tool(
        description = "Capture the current Android screen when visual context is required to answer a request about what is visible on-screen.",
    )
    fun take_screenshot(): Map<String, String> {
        return mapOf("status" to "handled_manually")
    }

    @Tool(
        description = "Insert generated text into the user's currently focused editable text field.",
    )
    fun inject_into_textfield(
        @ToolParam(description = "The exact text to insert into the active text field.")
        text: String,
    ): Map<String, String> {
        return mapOf(
            "status" to "handled_manually",
            "text" to text,
        )
    }
}
