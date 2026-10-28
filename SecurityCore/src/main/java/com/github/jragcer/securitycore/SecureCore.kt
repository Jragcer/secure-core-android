package com.github.jragcer.securitycore

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.github.jragcer.securitycore.api.biometric.BiometricAuthManager
import com.github.jragcer.securitycore.api.crypto.AsymmetricAlgorithm
import com.github.jragcer.securitycore.api.crypto.AsymmetricCryptoManager
import com.github.jragcer.securitycore.api.crypto.CryptoManager
import com.github.jragcer.securitycore.api.integrity.IntegrityManager
import com.github.jragcer.securitycore.api.network.NetworkSecurityManager
import com.github.jragcer.securitycore.api.screen.ScreenProtectionManager
import com.github.jragcer.securitycore.api.storage.PassphraseManager
import com.github.jragcer.securitycore.api.storage.SecureFileStorage
import com.github.jragcer.securitycore.api.storage.SecurePreferences
import com.github.jragcer.securitycore.api.tamper.TamperProtectionManager
import com.github.jragcer.securitycore.internal.biometric.BiometricAuthManagerImpl
import com.github.jragcer.securitycore.internal.crypto.AsymmetricCryptoManagerImpl
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
     *
     * This only reports device support; it does not make the library's keys StrongBox-backed.
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
     * Detection is heuristic and can be bypassed by a determined attacker.
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
     * The check runs on the client and can be bypassed on a compromised device.
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
     * Creates and initializes a [NetworkSecurityManager] to manage SSL/TLS certificate pinning
     * and to inspect the device trust store for user-installed CAs (a common sign of
     * traffic-interception proxies).
     *
     * @param context Application context.
     * @return Configured [NetworkSecurityManager] instance.
     */
    fun createNetworkSecurityManager(context: Context): NetworkSecurityManager {
        return NetworkSecurityManagerImpl(context.applicationContext)
    }

    /**
     * Creates an instance of [CryptoManager] backed by the AndroidKeyStore.
     *
     * Uses AES-256-GCM for authenticated encryption of byte arrays and data streams.
     * Keys are hardware-backed only on devices that support it.
     *
     * @param keyAlias The alias under which the secret key is created or retrieved.
     * @return A configured [CryptoManager] implementation.
     */
    fun createCryptoManager(keyAlias: String = "AppMasterKey"): CryptoManager {
        return KeystoreCryptoManager(keyAlias)
    }

    /**
     * Kept for source compatibility. The [provider] value was never applied: the
     * AndroidKeyStore is always used.
     */
    @Deprecated(
        message = "The provider parameter was never applied; the AndroidKeyStore is always used.",
        replaceWith = ReplaceWith("createCryptoManager(keyAlias)")
    )
    @Suppress("UNUSED_PARAMETER")
    fun createCryptoManager(keyAlias: String, provider: String = KeystoreCryptoManager.KEYSTORE_PROVIDER): CryptoManager {
        return KeystoreCryptoManager(keyAlias)
    }

    /**
     * Creates an instance of [AsymmetricCryptoManager] backed by the AndroidKeyStore.
     *
     * The key pair is generated on first use of an alias, which can take noticeable time for RSA:
     * call this from a background thread. If the alias already holds a key of a different type or
     * size, an [IllegalStateException] is thrown. One key pair per alias is used for all operations;
     * use different aliases to keep encryption and signing keys separate.
     *
     * @param alias Unique KeyStore alias used to persist the asymmetric key pair.
     * @param algorithm Asymmetric algorithm configuration (defaults to RSA 2048-bit with OAEP padding).
     * @return A thread-safe [AsymmetricCryptoManager] instance.
     */
    fun createAsymmetricCryptoManager(
        alias: String,
        algorithm: AsymmetricAlgorithm = AsymmetricAlgorithm.RSA()
    ): AsymmetricCryptoManager {
        return AsymmetricCryptoManagerImpl(alias, algorithm)
    }
}