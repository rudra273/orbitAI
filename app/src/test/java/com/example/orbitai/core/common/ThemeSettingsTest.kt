package com.example.orbitai.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeSettingsTest {

    @Test
    fun existingUsersKeepTheirChoiceAndNewInstallsFollowTheSystem() {
        assertEquals(ThemeMode.DARK, ThemeSettingsStore.resolveMode(stored = null, legacyDark = true))
        assertEquals(ThemeMode.LIGHT, ThemeSettingsStore.resolveMode(stored = null, legacyDark = false))
        assertEquals(ThemeMode.SYSTEM, ThemeSettingsStore.resolveMode(stored = null, legacyDark = null))
        assertEquals(ThemeMode.BLACK, ThemeSettingsStore.resolveMode(stored = "BLACK", legacyDark = false))
        assertEquals(ThemeMode.DARK, ThemeSettingsStore.resolveMode(stored = "bogus", legacyDark = true))
    }

    @Test
    fun darkDependsOnMode() {
        assertTrue(Appearance(ThemeMode.SYSTEM).isDark(systemDark = true))
        assertFalse(Appearance(ThemeMode.SYSTEM).isDark(systemDark = false))
        assertTrue(Appearance(ThemeMode.BLACK).isDark(systemDark = false))
        assertFalse(Appearance(ThemeMode.LIGHT).isDark(systemDark = true))
    }
}
