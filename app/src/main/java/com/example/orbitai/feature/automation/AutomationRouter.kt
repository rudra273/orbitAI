package com.example.orbitai.feature.automation

import com.example.orbitai.feature.automation.parser.AutomationCommandParser
import com.example.orbitai.feature.automation.parser.AutomationRequest

sealed interface AutomationRoute {
    data object NormalChat : AutomationRoute
    data class ToolOnly(val request: AutomationRequest) : AutomationRoute
}

object AutomationRouter {

    fun route(input: String): AutomationRoute {
        val request = AutomationCommandParser.parse(input)
        return if (request != null) {
            AutomationRoute.ToolOnly(request)
        } else {
            AutomationRoute.NormalChat
        }
    }
}
