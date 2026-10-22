package com.github.jragcer.securitycore.integrity

import com.github.jragcer.securitycore.api.integrity.ThreatType
import com.github.jragcer.securitycore.internal.integrity.EmulatorDetector
import com.github.jragcer.securitycore.internal.integrity.HookDetector
import com.github.jragcer.securitycore.internal.integrity.IntegrityManagerImpl
import com.github.jragcer.securitycore.internal.integrity.RootDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class IntegrityManagerUnitTest {
    private lateinit var mockRootDetector: RootDetector
    private lateinit var mockEmulatorDetector: EmulatorDetector
    private lateinit var mockHookDetector: HookDetector

    @Before
    fun setUp(){
        mockRootDetector = mock()
        mockEmulatorDetector = mock()
        mockHookDetector = mock()
    }

    private fun createManager(): IntegrityManagerImpl {
        return IntegrityManagerImpl(
            context = null,
            rootDetector = mockRootDetector,
            emulatorDetector = mockEmulatorDetector,
            hookDetector = mockHookDetector
        )
    }

    @Test
    fun isDeviceCompromised_returnsFalse_whenAllDetectorsAreClean() {
        `when`(mockRootDetector.isRooted()).thenReturn(false)
        `when`(mockEmulatorDetector.isEmulator()).thenReturn(false)
        `when`(mockHookDetector.isHookingOrDebuggerActive()).thenReturn(false)

        val manager = createManager()
        assertFalse(manager.isDeviceCompromised())
    }

    @Test
    fun isDeviceCompromised_returnsTrue_whenRootDetected() {
        `when`(mockRootDetector.isRooted()).thenReturn(true)
        `when`(mockEmulatorDetector.isEmulator()).thenReturn(false)
        `when`(mockHookDetector.isHookingOrDebuggerActive()).thenReturn(false)

        val manager = createManager()
        assertTrue(manager.isDeviceCompromised())
        assertTrue(manager.isRooted())
    }

    @Test
    fun isDeviceCompromised_returnsTrue_whenEmulatorDetected() {
        `when`(mockRootDetector.isRooted()).thenReturn(false)
        `when`(mockEmulatorDetector.isEmulator()).thenReturn(true)
        `when`(mockHookDetector.isHookingOrDebuggerActive()).thenReturn(false)

        val manager = createManager()
        assertTrue(manager.isDeviceCompromised())
        assertTrue(manager.isEmulator())
    }

    @Test
    fun isDeviceCompromised_returnsTrue_whenDebuggerOrHookingDetected() {
        `when`(mockRootDetector.isRooted()).thenReturn(false)
        `when`(mockEmulatorDetector.isEmulator()).thenReturn(false)
        `when`(mockHookDetector.isHookingOrDebuggerActive()).thenReturn(true)

        val manager = createManager()
        assertTrue(manager.isDeviceCompromised())
        assertTrue(manager.isDebuggerOrHookingActive())
    }

    @Test
    fun getIntegrityReport_returnsSecureReport_whenNoThreatsPresent() {
        `when`(mockRootDetector.isRooted()).thenReturn(false)
        `when`(mockEmulatorDetector.isEmulator()).thenReturn(false)
        `when`(mockHookDetector.isDebuggerConnected()).thenReturn(false)
        `when`(mockHookDetector.isAppDebuggable()).thenReturn(false)
        `when`(mockHookDetector.checkMemoryMapsForHookingLibraries()).thenReturn(false)

        val manager = createManager()
        val report = manager.getIntegrityReport()

        assertTrue(report.isSecure)
        assertTrue(report.detectedThreats.isEmpty())
        assertFalse(report.details["is_rooted"] ?: true)
        assertFalse(report.details["is_emulator"] ?: true)
        assertFalse(report.details["debugger_connected"] ?: true)
        assertFalse(report.details["app_debuggable"] ?: true)
        assertFalse(report.details["hooking_framework_detected"] ?: true)
    }

    @Test
    fun getIntegrityReport_aggregatesMultipleThreatsCorrectly() {
        `when`(mockRootDetector.isRooted()).thenReturn(true)
        `when`(mockEmulatorDetector.isEmulator()).thenReturn(true)
        `when`(mockHookDetector.isDebuggerConnected()).thenReturn(true)
        `when`(mockHookDetector.isAppDebuggable()).thenReturn(false)
        `when`(mockHookDetector.checkMemoryMapsForHookingLibraries()).thenReturn(true)

        val manager = createManager()
        val report = manager.getIntegrityReport()

        assertFalse(report.isSecure)
        assertEquals(4, report.detectedThreats.size)
        assertTrue(report.detectedThreats.contains(ThreatType.ROOT_DETECTED))
        assertTrue(report.detectedThreats.contains(ThreatType.EMULATOR_DETECTED))
        assertTrue(report.detectedThreats.contains(ThreatType.DEBUGGER_ATTACHED))
        assertTrue(report.detectedThreats.contains(ThreatType.HOOKING_FRAMEWORK_DETECTED))
    }
}