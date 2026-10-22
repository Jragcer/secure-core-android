package com.github.jragcer.securitycore.internal.integrity

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Debug
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

/**
 *  Internal heuristi detector for active debuggers and dynamic instrumentation frameworks
 *  (e.g., Frida, Xposed, Cydia Substrate).
 */
internal class HookDetector (
    private val context: Context
) {

    /**
     *  Executes all debugger and hooking framework detection heuristics.
     *
     *  @return true if an active debugger or instrumentation tool is detected; false otherwise
     */
    fun isHookingOrDebuggerActive(): Boolean{
        return isDebuggerConnected() ||
                isAppDebuggable() ||
                checkMemoryMapsForHookingLibraries() ||
                checkFridaDefaultPort()
    }

    /**
     *  Checks if a debugger is actively attached to the process or waiting to attach.
     *
     *  @return true if a debugger is connected; false otherwise
     */
    fun isDebuggerConnected(): Boolean{
        return Debug.isDebuggerConnected() || Debug.waitingForDebugger()
    }

    /**
     *  Verifies if the application manifest was compiled with the android:debuggable flag set to true.
     *
     *  @return true if the application is debuggable; false otherwise
     */
    fun isAppDebuggable(): Boolean{
        return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    /**
     *  Scans the process memory mapping file (/proc/self/maps) searching for signatures
     *  of hooking engines, dynamic instrumentation gadgets, or shared objects.
     *
     *  @return true if any known hooking framework is detected; false otherwise
     */
    fun checkMemoryMapsForHookingLibraries(): Boolean {
        val mapsFile = File("/proc/self/maps")
        if (!mapsFile.exists() || !mapsFile.canRead()) {
            return false
        }

        val suspiciousSignatures = arrayOf(
            "frida",
            "frida-gadget",
            "frida-agent",
            "xposed",
            "substrate",
            "com.saurik.substrate",
            "edxposed",
            "lsposed"
        )

        return try {
           BufferedReader(FileReader(mapsFile)).use { reader ->
               var line: String? = reader.readLine()
               while (line != null) {
                   val lowerLine = line.lowercase()
                   for (signature in suspiciousSignatures) {
                       if (lowerLine.contains(signature)) {
                           return true
                       }
                   }
                   line = reader.readLine()
               }
               false
           }
        } catch (_: Throwable) {
            false
        }
    }

    /**
     * Attempts to establish a quick connection to localhost on the standard Frida server port (27042).
     *
     * @return true if frida is running on the default port; false otherwise
     */
    private fun checkFridaDefaultPort(): Boolean{
        val defaultPort = 27042
        return try {
            Socket().use{ socket ->
                socket.connect(InetSocketAddress("127.0.0.1",defaultPort), 100)
                true
            }
        }catch (_: Throwable){
            false
        }
    }
}