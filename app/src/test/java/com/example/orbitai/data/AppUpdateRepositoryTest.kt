package com.example.orbitai.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AppUpdateRepositoryTest {
    @Test
    fun compareVersions_detectsNewerPatchRelease() {
        assertEquals(1, AppUpdateRepository.compareVersions("1.2.4", "1.2.3"))
    }

    @Test
    fun compareVersions_handlesLeadingVPrefix() {
        assertEquals(0, AppUpdateRepository.compareVersions("v1.2.3", "1.2.3"))
    }

    @Test
    fun compareVersions_handlesDifferentLengthVersions() {
        assertEquals(-1, AppUpdateRepository.compareVersions("1.2", "1.2.1"))
    }
}
