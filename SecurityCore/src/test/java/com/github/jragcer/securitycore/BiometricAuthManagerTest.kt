package com.github.jragcer.securitycore

import android.content.Context
import androidx.biometric.BiometricPrompt
import androidx.test.core.app.ApplicationProvider
import com.github.jragcer.securitycore.api.biometric.BiometricResult
import com.github.jragcer.securitycore.api.biometric.BiometricStatus
import com.github.jragcer.securitycore.internal.biometric.BiometricAuthManagerImpl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class BiometricAuthManagerTest {
    private lateinit var context: Context
    private lateinit var manager: BiometricAuthManagerImpl

    @Before
    fun setUp(){
        context = ApplicationProvider.getApplicationContext()
        manager = BiometricAuthManagerImpl(context)
    }

    @Test
    fun factory_creates_valid_BiometricAuthManager_instance(){
        val created = SecureCore.createBiometricAuthManager(context)
        assertNotNull(created)
        assertTrue(created is BiometricAuthManagerImpl)
    }

    @Test
    fun canAuthenticate_returns_valid_BiometricStatus(){
        val status = manager.canAuthenticate(allowDeviceCredential = false)

        assertNotNull(status is BiometricStatus.Ready ||
        status is BiometricStatus.NotEnrolled ||
        status is BiometricStatus.HardwareUnavailable ||
        status is BiometricStatus.NoHardware )
    }

    @Test
    fun biometricResult_success_holds_optional_CryptoObject(){
        val result = BiometricResult.Success(null)
        assertEquals(null, result.cryptoObject)
    }

    @Test
    fun biometricResult_error_retains_code_and_message(){
        val result = BiometricResult.Error(
            BiometricPrompt.ERROR_LOCKOUT,
            "Too many attemps")
        assertEquals(BiometricPrompt.ERROR_LOCKOUT, result.errorCode)
        assertEquals("Too many attemps", result.errorMessage)
    }

}