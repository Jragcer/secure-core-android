package com.github.jragcer.securitycore.api.integrity

/**
 *  Public interface for assessing device integrity and runtime threat levels.
 *
 *  Provides evaluation mechanisms against rooting, virtualization/emulation,
 *  attached debuggers, and dynamic binary instrumentation (e.g., Frida, Xposed).
 */
interface IntegrityManager {
    /**
     * Performs a fast composite evaluation to check whether the runtime environment
     * is compromised by any detected security threat.
     *
     * @return `true` if any root, emulator, debugger, or hooking framework indicator
     * is found; `false` if the device environment is considered safe.
     */
    fun isDeviceCompromised(): Boolean
    /**
     * Checks specifically for operating system tampering and administrative privileges (Root).
     *
     * This evaluates standard su binaries, dangerous system mount flags, custom ROM
     * test-keys, and known Superuser management packages.
     *
     * @return `true` if rooting indicators are detected; `false` otherwise.
     */
    fun isRooted(): Boolean
    /**
     * Checks whether the application is running inside a virtualized environment or emulator.
     *
     * Examines hardware properties (e.g., goldfish, ranchu), build fingerprints,
     * and QEMU communication drivers/pipes.
     *
     * @return `true` if executing in an emulator; `false` on physical hardware.
     */
    fun isEmulator(): Boolean
    /**
     * Verifies if the process is being actively monitored, debugged, or hooked in memory.
     *
     * Evaluates active debugger attachments, application debuggable flags,
     * memory maps (`/proc/self/maps`) for suspicious libraries, and default Frida ports.
     *
     * @return `true` if an active debugger or hooking engine is detected; `false` otherwise.
     */
    fun isDebuggerOrHookingActive(): Boolean
    /**
     * Generates a comprehensive diagnostic report detailing all evaluated heuristics.
     *
     * @return [IntegrityReport] containing the overall safety status, a list of active
     * [ThreatType] flags, and an itemized map of individual check results.
     */
    fun getIntegrityReport(): IntegrityReport
}