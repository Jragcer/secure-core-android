package com.github.jragcer.securitycore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.jragcer.securitycore.api.crypto.AsymmetricAlgorithm
import com.github.jragcer.securitycore.internal.crypto.KeystoreCryptoManager
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SecureCoreInstrumentedTest {
    private lateinit var appContext: Context

    @Before
    fun setUp() {
        appContext = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun strongBoxSupport_canBeQueriedWithoutCrash() {
        // En emulador API 29 evalúa la rama Build.VERSION.SDK_INT >= 28 sin errores
        val supported = SecureCore.isStrongBoxSupported(appContext)
        // Solo comprobamos que la llamada complete y devuelva un booleano válido
        assertTrue(supported || !supported)
    }

    @Test
    fun createBiometricAuthManager_instantiatesSuccessfully() {
        val manager = SecureCore.createBiometricAuthManager(appContext)
        assertNotNull(manager)
    }

    @Test
    fun createPassphraseManager_instantiatesSuccessfully() {
        val manager = SecureCore.createPassphraseManager(appContext)
        assertNotNull(manager)
    }

    @Test
    fun createSecureFileStorage_instantiatesSuccessfully() {
        val manager = SecureCore.createSecureFileStorage(appContext)
        assertNotNull(manager)
    }

    @Test
    fun createIntegrityManager_instantiatesSuccessfully() {
        val manager = SecureCore.createIntegrityManager(appContext)
        assertNotNull(manager)
    }

    @Test
    fun createTamperProtectionManager_instantiatesSuccessfully() {
        val manager = SecureCore.createTamperProtectionManager(appContext)
        assertNotNull(manager)
    }

    @Test
    fun createNetworkSecurityManager_instantiatesSuccessfully() {
        val manager = SecureCore.createNetworkSecurityManager(appContext)
        assertNotNull(manager)
    }

    @Test
    fun createSecurePreferences_worksWithDefaultAndCustomFileName() {
        // Sobrecarga por defecto (1 parámetro)
        val defaultResult = SecureCore.createSecurePreferences(appContext)
        assertNotNull(defaultResult)

        // Parámetro explícito (2 parámetros)
        val customResult = SecureCore.createSecurePreferences(appContext, "test_custom_prefs")
        assertNotNull(customResult)
    }

    @Test
    fun createCryptoManager_worksWithDefaultAndCustomAlias() {
        // Sobrecarga por defecto
        val defaultManager = SecureCore.createCryptoManager()
        assertNotNull(defaultManager)

        // Alias explícito
        val customManager = SecureCore.createCryptoManager("MyCustomAlias")
        assertNotNull(customManager)
    }

    @Suppress("DEPRECATION")
    @Test
    fun createCryptoManager_deprecatedVariant_worksCorrectly() {
        val managerWithDefaultProvider = SecureCore.createCryptoManager("DeprecatedKeyAlias")
        assertNotNull(managerWithDefaultProvider)

        val managerWithExplicitProvider = SecureCore.createCryptoManager(
            keyAlias = "DeprecatedKeyAlias2",
            provider = KeystoreCryptoManager.KEYSTORE_PROVIDER
        )
        assertNotNull(managerWithExplicitProvider)
    }

    @Test
    fun createAsymmetricCryptoManager_worksWithDefaultAndCustomAlgorithms() {
        // Sobrecarga por defecto (RSA)
        val rsaManager = SecureCore.createAsymmetricCryptoManager("test_rsa_factory_alias")
        assertNotNull(rsaManager)

        // Parámetro explícito (EC)
        val ecManager = SecureCore.createAsymmetricCryptoManager(
            alias = "test_ec_factory_alias",
            algorithm = AsymmetricAlgorithm.EC()
        )
        assertNotNull(ecManager)
    }
}