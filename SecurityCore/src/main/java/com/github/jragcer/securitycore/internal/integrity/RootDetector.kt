package com.github.jragcer.securitycore.internal.integrity

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 *  Internal heuristic detector for rooting indicators, custom ROM test-keys,
 *  dangerous binaries, and write-permission anomalies in system partitions.
 */
internal class RootDetector {

    /**
     *  Executes all root detection heuristics
     *
     *  @return true if any known rooting indicator is found; false otherwise
     */
    fun isRooted(): Boolean{

        return checkTestKeys() ||
                checkKnownRootBinaries() ||
                checkSuExecutableExecution() ||
                checkDangerousProperties()
    }

    /**
     *  Verifies if the OS image was signed with release-keys or test-keys.
     *  Custom ROMs and rooted factory images typically contain "test-keys".
     *
     *  @return true if the OS image was signed with test-keys; false otherwise
     */
    private fun checkTestKeys(): Boolean{
        val buildTags = android.os.Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }

    /**
     * Inspects standard binary directories searching for known rooting artifacts such as su,
     * busybox, magisk or superuser binaries
     *
     * @return true if any known root binaries are found; false otherwise
     */
    private fun checkKnownRootBinaries(): Boolean {
        val candidatePaths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su",
            "/system/xbin/busybox",
            "/system/bin/busybox"
        )

        for (path in candidatePaths) {
            if (File(path).exists()) {
                return true
            }
        }
        return false
    }

    /**
     * Attempts to query the `which su` command in a non-blocking process execution.
     * Even if su binary paths are masked, the PATH environment variable might resolve it.
     *
     * @return true if the `which su` command is found; false otherwise
     */
    private fun checkSuExecutableExecution(): Boolean{
        var process: Process? = null

        return try {
            process = Runtime.getRuntime().exec(arrayOf("which","su"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            reader.readLine() != null
        }catch (_: Throwable){
            false
        }finally {
            process?.destroy()
        }
    }

    /**
     *  Checks runtime system properties that are commonly altered on rooted devices
     *  (e.g., debuggable build tags or root daemon states)
     *
     *  @return true if any known dangerous properties are found; false otherwise
     */
    private fun checkDangerousProperties(): Boolean{
        val debuggable = getSystemProperty("ro.debuggable")
        val secure = getSystemProperty("ro.secure")

        // In standard production builds: ro.secure == "1" and ro.debuggable == "0"
        return debuggable == "1" || secure == "0"
    }

    /**
     * Safely queries system properties via the `getprop` command line utility
     *
     * @return the value of the system property or an empty string if not found
     */
    private fun getSystemProperty(propName: String): String{
        var process: Process? = null

        return try {
            process = Runtime.getRuntime().exec(arrayOf("getprop", propName))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            reader.readLine()?.trim() ?: ""
        }catch (_: Throwable){
            ""
        }finally {
            process?.destroy()
        }
    }
}