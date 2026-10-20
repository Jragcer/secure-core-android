package com.github.jragcer.securitycore

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

/**
 * Entry point providing library metadata and runtime information.
 */
object SecureCore {

    const val VERSION: String = BuildConfig.SDK_VERSION
    const val LIBRARY: String = "SecureCore"

    /**
     * Returns the library version .
     */
    fun getVersion(): String = VERSION

    /**
     * Returns a formatted descriptor including name and semantic version.
     */
    fun getVersionName(): String = "$LIBRARY v$VERSION"

    /**
     * Checks whether the device hardware supports StrongBox Keymaster (dedicated tamper-resistant HSM).
     * Available on Android 9.0 (API level 28) and above.
     */
    fun isStrongBoxSupported(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_STRONGBOX_KEYSTORE)
        } else {
            false
        }
    }
}