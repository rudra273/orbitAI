package com.example.orbitai.tools.router

import com.example.orbitai.tools.intents.IntentToolCommandParser
import com.example.orbitai.tools.intents.IntentToolRequest

sealed interface ToolRoute {
    data object NormalChat : ToolRoute
    data class ToolOnly(val request: IntentToolRequest) : ToolRoute
}

object ToolRouter {

    fun route(input: String): ToolRoute {
        val request = IntentToolCommandParser.parse(input)
        return if (request != null) {
            ToolRoute.ToolOnly(request)
        } else {
            ToolRoute.NormalChat
        }
    }
}
