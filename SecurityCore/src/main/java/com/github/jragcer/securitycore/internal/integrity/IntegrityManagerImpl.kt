package com.github.jragcer.securitycore.internal.integrity

import android.content.Context
import com.github.jragcer.securitycore.api.integrity.IntegrityManager
import com.github.jragcer.securitycore.api.integrity.IntegrityReport
import com.github.jragcer.securitycore.api.integrity.ThreatType

/**
 * Internal implementation of [IntegrityManager].
 *
 * Coordinates heuristic detectors ([RootDetector], [EmulatorDetector], and [HookDetector])
 * to provide consolidated security verifications and diagnostic reports.
 *
 * @param context Application context used for system property and package queries.
 * @param rootDetector Detector handling operating system root artifacts and test keys.
 * @param emulatorDetector Detector identifying virtualized sandbox environments.
 * @param hookDetector Detector inspecting memory maps, ports, and debugger states.
 */
internal class IntegrityManagerImpl (
    context: Context?,
    private val rootDetector: RootDetector = RootDetector(),
    private val emulatorDetector: EmulatorDetector = EmulatorDetector(context?.applicationContext ?: context!!),
    private val hookDetector: HookDetector = HookDetector(context?.applicationContext ?: context!!)
) : IntegrityManager {

    /**
     * Performs a fast composite evaluation to check whether the runtime environment
     * is compromised by any detected security threat.
     *
     * @return `true` if any root, emulator, debugger, or hooking framework indicator
     * is found; `false` if the device environment is considered safe.
     */
    override fun isDeviceCompromised(): Boolean {
        return isRooted() || isEmulator() || isDebuggerOrHookingActive()
    }

    /**
     * Checks specifically for operating system tampering and administrative privileges (Root).
     *
     * This evaluates standard su binaries, dangerous system mount flags, custom ROM
     * test-keys, and known Superuser management packages.
     *
     * @return `true` if rooting indicators are detected; `false` otherwise.
     */
    override fun isRooted(): Boolean {
        return rootDetector.isRooted()
    }

    /**
     * Checks whether the application is running inside a virtualized environment or emulator.
     *
     * Examines hardware properties (e.g., goldfish, ranchu), build fingerprints,
     * and QEMU communication drivers/pipes.
     *
     * @return `true` if executing in an emulator; `false` on physical hardware.
     */
    override fun isEmulator(): Boolean {
        return emulatorDetector.isEmulator()
    }

    /**
     * Verifies if the process is being actively monitored, debugged, or hooked in memory.
     *
     * Evaluates active debugger attachments, application debuggable flags,
     * memory maps (`/proc/self/maps`) for suspicious libraries, and default Frida ports.
     *
     * @return `true` if an active debugger or hooking engine is detected; `false` otherwise.
     */
    override fun isDebuggerOrHookingActive(): Boolean {
        return hookDetector.isHookingOrDebuggerActive()
    }

    /**
     * Generates a comprehensive diagnostic report detailing all evaluated heuristics.
     *
     * @return [IntegrityReport] containing the overall safety status, a list of active
     * [ThreatType] flags, and an itemized map of individual check results.
     */
    override fun getIntegrityReport(): IntegrityReport {
        val detectedThreats = mutableListOf<ThreatType>()
        val details = mutableMapOf<String, Boolean>()

        // 1.Evaluate root indicators and system tampering
        val rooted = rootDetector.isRooted()
        details["is_rooted"] = rooted
        if(rooted){
            detectedThreats.add(ThreatType.ROOT_DETECTED)
        }

        // 2.Evaluate virtualization and emulation profiles
        val emulator = emulatorDetector.isEmulator()
        details["is_emulator"] = emulator
        if(emulator){
            detectedThreats.add(ThreatType.EMULATOR_DETECTED)
        }

        // 3.Evaluate active debugger attachments and debuggable build flags
        val debuggerConnected = hookDetector.isDebuggerConnected()
        val appDebuggable = hookDetector.isAppDebuggable()
        details["debugger_connected"] = debuggerConnected
        details["app_debuggable"] = appDebuggable
        if(debuggerConnected || appDebuggable){
            detectedThreats.add(ThreatType.DEBUGGER_ATTACHED)
        }

        // 4.Evaluate dynamic instrumentation and memory hooking signatures
        val hookingActive = hookDetector.checkMemoryMapsForHookingLibraries()
        details["hooking_framework_detected"] = hookingActive
        if(hookingActive){
            detectedThreats.add(ThreatType.HOOKING_FRAMEWORK_DETECTED)
        }

        // The environment is safe onlyif no theats were added
        val isSecure = detectedThreats.isEmpty()

        return IntegrityReport(
            isSecure = isSecure,
            detectedThreats = detectedThreats,
            details = details
        )
    }
}