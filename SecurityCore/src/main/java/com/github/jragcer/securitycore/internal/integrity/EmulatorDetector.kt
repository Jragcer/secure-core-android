package com.github.jragcer.securitycore.internal.integrity

import android.content.Context
import android.os.Build
import java.io.File

/**
 * Internal heuristic detector for virtualized environments, emulators, and sandboxes
 * (e.g., QEMU, Android Studio AVD, Genymotion, BlueStacks, Nox)
 */
internal class EmulatorDetector (
    private val context: Context
) {

    /**
     *  Executes all emulator detection heuristics.
     *
     *  @return true if any known virtualization or emulator indicator is found; false otherwise
     */
    fun isEmulator(): Boolean{
        return checkBuildCharacteristics() ||
                checkHardwarePipesAndDrivers() ||
                checkQemuDrivers()
    }

    /**
     * Inspects standar Android Build metadata known to match default emulator configurations.
     *
     * @return true if any known emulator configuration is detected; false otherwise
     */
    private fun checkBuildCharacteristics(): Boolean{
        val fingerprint = Build.FINGERPRINT.lowercase()
        val model = Build.MODEL.lowercase()
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        val device = Build.DEVICE.lowercase()
        val product = Build.PRODUCT.lowercase()
        val hardware = Build.HARDWARE.lowercase()

        return fingerprint.startsWith("generic") ||
                fingerprint.startsWith("unknown") ||
                fingerprint.contains("google_sdk") ||
                fingerprint.contains("emulator") ||
                fingerprint.contains("vbox") ||
                model.contains("google_sdk") ||
                model.contains("emulator") ||
                model.contains("android sdk built for") ||
                model.contains("droid4x") ||
                manufacturer.contains("genymotion") ||
                manufacturer.contains("nox") ||
                brand.startsWith("generic") && device.startsWith("generic") ||
                hardware.contains("goldfish") ||
                hardware.contains("ranchu") ||
                hardware.contains("vbox86") ||
                hardware.contains("nox") ||
                product.contains("sdk") ||
                product.contains("google_sdk") ||
                product.contains("sdk_x86") ||
                product.contains("vbox86p") ||
                product.contains("emulator") ||
                product.contains("simulator")
    }

    /**
     *  Detects communication pipes and Unix domain sockets created exclusively by QEMU.
     *
     *  @return true if any known QEMU virtual environment is detected; false otherwise
     */
    private fun checkHardwarePipesAndDrivers(): Boolean{
        val candidatePipes = arrayOf(
            "/dev/socket/qemud",
            "/dev/qemu_pipe",
            "/system/lib/libc_malloc_debug_qemu.so",
            "/sys/qemu_trace"
        )
        for (pipePath in candidatePipes) {
            if (File(pipePath).exists()) {
                return true
            }
        }
        return false
    }

    /**
     *  Reads /proc/tty/drivers to inspect active QEMU or virtualization drivers
     *
     *  @return true if any known QEMU virtual environment is detected; false otherwise
     */
    private fun checkQemuDrivers(): Boolean{
        val driversFile= File("/proc/tty/drivers")
        if(!driversFile.exists() || !driversFile.canRead()){
            return false
        }

        return try {
            driversFile.useLines { lines ->
                lines.any { line -> line.contains("goldfish") }
            }
        } catch (_: Throwable){
            false
        }
    }
}