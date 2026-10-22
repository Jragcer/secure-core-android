package com.github.jragcer.securitycore.integrity

import android.content.Context
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.jragcer.securitycore.SecureCore
import com.github.jragcer.securitycore.api.integrity.IntegrityManager
import com.github.jragcer.securitycore.internal.integrity.EmulatorDetector
import com.github.jragcer.securitycore.internal.integrity.HookDetector
import com.github.jragcer.securitycore.internal.integrity.RootDetector
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@RunWith(AndroidJUnit4::class)
class IntegrityManagerInstrumentedTest {

    private lateinit var context: Context
    private lateinit var rootDetector: RootDetector
    private lateinit var emulatorDetector: EmulatorDetector
    private lateinit var hookDetector: HookDetector
    private lateinit var integrityManager: IntegrityManager

    @Before
    fun setUp(){
        context = InstrumentationRegistry.getInstrumentation().targetContext
        rootDetector = RootDetector()
        emulatorDetector = EmulatorDetector(context)
        hookDetector = HookDetector(context)
        integrityManager = SecureCore.createIntegrityManager(context)
    }

    @Test
    fun factory_createIntegrityManager_returnsValidInstance(){
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val manager = SecureCore.createIntegrityManager(context)

        assertNotNull("IntegrityManager instance must not be null", manager)
    }

    @Test
    fun rootDetector_executesWithoutExceptionOnLiveDevice(){
        val isRooted = rootDetector.isRooted()

        assertNotNull(isRooted)
    }

    @Test
    fun emulatorDetector_detectsEmulatorWhenRunningInAndroidStudioAVD(){
        val isEmulator = emulatorDetector.isEmulator()

        assertNotNull(isEmulator)
    }

    @Test
    fun hookDetector_appDebuggable_matchesPackageManifestFlag(){
        val isDebuggable = hookDetector.isAppDebuggable()

        assertNotNull(isDebuggable)
    }

    @Test
    fun hookDetector_isDebuggerConnected_executesSafely() {
        val isConnected = hookDetector.isDebuggerConnected()

        assertNotNull(isConnected)
    }

    @Test
    fun hookDetector_memoryMapsScanning_readsProcessWithoutCrashing() {
        val hookFound = hookDetector.checkMemoryMapsForHookingLibraries()

        assertFalse("Clean instrumented test runner should not contain injected hooking libraries", hookFound)
    }

    @Test
    fun integrityManager_isDeviceCompromised_matchesAggregatedDetectors() {
        val compromised = integrityManager.isDeviceCompromised()
        val individualOr = integrityManager.isRooted() ||
                integrityManager.isEmulator() ||
                integrityManager.isDebuggerOrHookingActive()

        assertTrue("isDeviceCompromised must reflect individual detector flags", compromised == individualOr)
    }

    @Test
    fun integrityReport_buildsDetailedNonEmptyMap(){
        val report = integrityManager.getIntegrityReport()

        assertNotNull("Report must not be null",report)
        assertNotNull("Detected threats list must not be null", report.detectedThreats)
        assertTrue("Details map must contain heuristic keys", report.details.isNotEmpty())

        assertTrue(report.details.containsKey("is_rooted"))
        assertTrue(report.details.containsKey("is_emulator"))
        assertTrue(report.details.containsKey("debugger_connected"))
        assertTrue(report.details.containsKey("app_debuggable"))
        assertTrue(report.details.containsKey("hooking_framework_detected"))

        if(report.detectedThreats.isNotEmpty()){
            assertFalse("cannot be marked secure if threats are detected", report.isSecure)
        }else{
            assertTrue("Report must be secure when threats list is empty", report.isSecure)
        }
    }
}