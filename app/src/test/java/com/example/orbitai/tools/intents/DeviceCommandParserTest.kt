package com.example.orbitai.tools.intents

import com.example.orbitai.feature.automation.parser.DeviceCommand
import com.example.orbitai.feature.automation.parser.DeviceCommandParser
import com.example.orbitai.feature.automation.parser.AutomationCommandParser
import com.example.orbitai.feature.automation.parser.AutomationRequest
import com.example.orbitai.feature.automation.executor.callableNumber
import org.junit.Assert.*
import org.junit.Test

class DeviceCommandParserTest {
    @Test fun recognizesVoiceCallingAndExplicitMemory() {
        assertEquals(DeviceCommand.Call("Mom"), DeviceCommandParser.parse("Hey Orbit, please call Mom"))
        assertEquals(DeviceCommand.Remember("my bike is parked on level 2"), DeviceCommandParser.parse("Remember that my bike is parked on level 2"))
        assertEquals(DeviceCommand.Remember("Buy milk\nGet bread"), DeviceCommandParser.parse("save this to memory: Buy milk\nGet bread"))
        assertNull(DeviceCommandParser.parse("What do you remember about me?"))
    }
    @Test fun routesPoliteReminderWithoutConfusingRemember() {
        val text = DeviceCommandParser.normalize("Orbit, can you please set a reminder at 10 pm")
        assertEquals(AutomationRequest.CreateReminder("at 10 pm"), AutomationCommandParser.parse(text))
        assertNull(DeviceCommandParser.parse("Remind me to call Mom at 10 pm"))
    }
    @Test fun acceptsFormattedNumbersButRejectsServiceCodesAndUris() {
        assertEquals("+919876543210", callableNumber("+91 (98765) 43210"))
        assertNull(callableNumber("*21*9876543210#"))
        assertNull(callableNumber("tel:9876543210"))
        assertNull(callableNumber("Mom"))
    }
}
