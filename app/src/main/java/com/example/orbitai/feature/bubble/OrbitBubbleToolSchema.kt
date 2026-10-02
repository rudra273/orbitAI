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
        description = "Request user-approved Android screen capture when visual context is required to answer a request about what is visible on-screen.",
    )
    fun take_screenshot(): Map<String, String> {
        return mapOf("status" to "handled_manually")
    }

    @Tool(
        description = "Copy generated text to the clipboard so the user can paste it into another app.",
    )
    fun copy_to_clipboard(
        @ToolParam(description = "The exact text to copy to the clipboard.")
        text: String,
    ): Map<String, String> {
        return mapOf(
            "status" to "handled_manually",
            "text" to text,
        )
    }
}
