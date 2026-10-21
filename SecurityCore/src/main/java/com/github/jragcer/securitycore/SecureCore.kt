package com.github.jragcer.securitycore

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.github.jragcer.securitycore.api.biometric.BiometricAuthManager
import com.github.jragcer.securitycore.api.storage.PassphraseManager
import com.github.jragcer.securitycore.api.storage.SecureFileStorage
import com.github.jragcer.securitycore.api.storage.SecurePreferences
import com.github.jragcer.securitycore.internal.biometric.BiometricAuthManagerImpl
import com.github.jragcer.securitycore.internal.storage.PassphraseManagerImpl
import com.github.jragcer.securitycore.internal.storage.SecureFileStorageImpl
import com.github.jragcer.securitycore.internal.storage.SecurePreferencesImpl

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

    /**
     * Creates an instance of [BiometricAuthManager] backed by AndroidX BiometricPrompt.
     */
    fun createBiometricAuthManager(context: Context): BiometricAuthManager{
        return BiometricAuthManagerImpl(context.applicationContext)
    }

    /**
     * Creates an instance of [PassphraseManager] backed by Android Keystore.
     */
    fun createPassphraseManager(context: Context): PassphraseManager {
        return PassphraseManagerImpl(context.applicationContext)
    }

    /**
     * Creates an instance of [SecurePreferences] backed by Android Keystore.
     */
    fun createSecurePreferences(
        context: Context,
        fileName: String = "app_secure_prefs"
    ): Result<SecurePreferences> {
        return SecurePreferencesImpl.create(context.applicationContext, fileName)
    }

    /**
     * Creates an instance of [SecureFileStorage] backed by Android Keystore.
     */
    fun createSecureFileStorage(context: Context): SecureFileStorage {
        return SecureFileStorageImpl(context.applicationContext)
    }
}