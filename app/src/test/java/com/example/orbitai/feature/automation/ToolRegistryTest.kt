package com.example.orbitai.feature.automation

import com.example.orbitai.feature.automation.tools.ToolRegistry
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolRegistryTest {

    @Test
    fun everyDefinitionProducesAnObjectSchemaWithItsParameters() {
        ToolRegistry.all.forEach { def ->
            val json = JsonParser.parseString(def.descriptionJson()).asJsonObject
            assertEquals(def.name, json["name"].asString)
            assertTrue(def.name, json["description"].asString.isNotBlank())
            val parameters = json["parameters"].asJsonObject
            assertEquals("object", parameters["type"].asString)
            assertEquals(def.parameters.map { it.name }.toSet(), parameters["properties"].asJsonObject.keySet())
            assertEquals(def.parameters.map { it.name }, parameters["required"].asJsonArray.map { it.asString })
        }
    }

    @Test
    fun numericParametersAreDescribedAsNumbers() {
        val timer = JsonParser.parseString(ToolRegistry.all.first { it.name == "set_timer" }.descriptionJson()).asJsonObject
        val props = timer["parameters"].asJsonObject["properties"].asJsonObject
        assertEquals("number", props["minutes"].asJsonObject["type"].asString)
        assertEquals("string", props["label"].asJsonObject["type"].asString)
    }

    @Test
    fun namesAreUniqueAndEachToolHasASwitchAndASlashCommand() {
        assertEquals(ToolRegistry.all.size, ToolRegistry.all.map { it.name }.toSet().size)
        assertEquals(ToolRegistry.all.size, ToolRegistry.all.map { it.tool }.toSet().size)
        ToolRegistry.all.forEach { def ->
            val command = def.tool.command
            assertNotNull(def.name, command)
            assertTrue(def.name, OrbitCommands.all.any { it.name == command })
        }
    }

    @Test
    fun everyOrbitToolIsOfferedToTheModel() {
        assertEquals(OrbitTool.entries.toSet(), ToolRegistry.all.map { it.tool }.toSet())
    }

    @Test
    fun onlyEnabledToolsAreOffered() {
        val offered = ToolRegistry.enabled(setOf(OrbitTool.TIMER, OrbitTool.CALLS)).map { it.name }
        assertEquals(listOf("call_contact", "set_timer"), offered.sorted())
        assertTrue(ToolRegistry.openApiTools(emptySet()).isEmpty())
        assertEquals(2, ToolRegistry.openApiTools(setOf(OrbitTool.TIMER, OrbitTool.CALLS)).size)
    }

    @Test
    fun theModelLibraryGetsTheSameJsonAndIsNeverAskedToRunTheTool() {
        val def = ToolRegistry.all.first { it.name == "call_contact" }
        val tool = def.asOpenApiTool()
        assertEquals(def.descriptionJson(), tool.getToolDescriptionJsonString())
        assertEquals("""{"status":"handled_manually"}""", tool.execute("{}"))
    }

    @Test
    fun unknownToolsAreRejected() {
        assertEquals(null, ToolRegistry.toAction("delete_everything", mapOf("x" to "y")))
    }
}
