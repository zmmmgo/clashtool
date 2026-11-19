package com.example.clashtool

import com.example.clashtool.update.UpdateManager
import org.junit.Assert.*
import org.junit.Test

class UpdateManagerTest {

    @Test
    fun testIsNewerVersion() {
        // 远程大于本地
        assertTrue(UpdateManager.isNewerVersion("1.1", "1.0"))
        assertTrue(UpdateManager.isNewerVersion("1.0.1", "1.0.0"))
        assertTrue(UpdateManager.isNewerVersion("2.0.0", "1.9.9"))
        assertTrue(UpdateManager.isNewerVersion("v1.2.0", "1.1.0"))
        assertTrue(UpdateManager.isNewerVersion("1.10.0", "1.9.0"))

        // 远程等于或小于本地
        assertFalse(UpdateManager.isNewerVersion("1.0", "1.0"))
        assertFalse(UpdateManager.isNewerVersion("1.0.0", "1.0.0"))
        assertFalse(UpdateManager.isNewerVersion("0.9.9", "1.0.0"))
        assertFalse(UpdateManager.isNewerVersion("1.0", "1.1"))
    }
}
