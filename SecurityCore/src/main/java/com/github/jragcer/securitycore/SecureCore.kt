package com.github.jragcer.securitycore

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.github.jragcer.securitycore.api.biometric.BiometricAuthManager
import com.github.jragcer.securitycore.api.crypto.CryptoManager
import com.github.jragcer.securitycore.api.integrity.IntegrityManager
import com.github.jragcer.securitycore.api.network.NetworkSecurityManager
import com.github.jragcer.securitycore.api.screen.ScreenProtectionManager
import com.github.jragcer.securitycore.api.storage.PassphraseManager
import com.github.jragcer.securitycore.api.storage.SecureFileStorage
import com.github.jragcer.securitycore.api.storage.SecurePreferences
import com.github.jragcer.securitycore.api.tamper.TamperProtectionManager
import com.github.jragcer.securitycore.internal.biometric.BiometricAuthManagerImpl
import com.github.jragcer.securitycore.internal.crypto.KeystoreCryptoManager
import com.github.jragcer.securitycore.internal.integrity.IntegrityManagerImpl
import com.github.jragcer.securitycore.internal.network.NetworkSecurityManagerImpl
import com.github.jragcer.securitycore.internal.screen.ScreenProtectionManagerImpl
import com.github.jragcer.securitycore.internal.storage.PassphraseManagerImpl
import com.github.jragcer.securitycore.internal.storage.SecureFileStorageImpl
import com.github.jragcer.securitycore.internal.storage.SecurePreferencesImpl
import com.github.jragcer.securitycore.internal.tamper.TamperProtectionManagerImpl

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

    /**
     * Creates an instance of [IntegrityManager] to assess runtime environment security.
     *
     * @param context The application or activity context.
     * @return Configured [IntegrityManager] instance.
     */
    fun createIntegrityManager(context: Context): IntegrityManager {
        return IntegrityManagerImpl(context.applicationContext)
    }

    /**
     * Creates an instance of [TamperProtectionManager] to detect APK modifications,
     * repackaging, unauthorized signing certificates, and untrusted installation sources.
     *
     * @param context The application or activity context.
     * @return Configured [TamperProtectionManager] instance.
     */
    fun createTamperProtectionManager(context: Context): TamperProtectionManager {
        return TamperProtectionManagerImpl(context.applicationContext)
    }

    /**
     * Creates an instance of [ScreenProtectionManager] to control
     * screen security, screenshot blocking, and app switcher privacy.
     *
     * @return An instantiated [ScreenProtectionManager].
     */
    fun createScreenProtectionManager(): ScreenProtectionManager {
        return ScreenProtectionManagerImpl()
    }

    /**
     * Creates and initializes a [NetworkSecurityManager] to manage SSL/TLS Certificate Pinning
     * and inspect the device trust store for user-installed CAs (MitM proxy detection).
     *
     * @param context Application context.
     * @return Configured [NetworkSecurityManager] instance.
     */
    fun createNetworkSecurityManager(context: Context): NetworkSecurityManager {
        return NetworkSecurityManagerImpl(context.applicationContext)
    }

    /**
     * Creates an instance of [CryptoManager] backed by hardware security (AndroidKeyStore).
     *
     * Uses AES-256-GCM for authenticated encryption, ensuring confidentiality and integrity
     * across byte arrays and data streams.
     *
     * @param keyAlias The alias under which the master secret key is created or retrieved.
     * @param provider The JCA provider name, defaulting to "AndroidKeyStore".
     * @return A configured [CryptoManager] implementation.
     */
    fun createCryptoManager(keyAlias: String = "AppMasterKey",
                            provider: String = KeystoreCryptoManager.KEYSTORE_PROVIDER): CryptoManager {
        return KeystoreCryptoManager(keyAlias)
    }
}