package com.github.jragcer.securitycore.internal.biometric

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.github.jragcer.securitycore.api.biometric.BiometricAuthManager
import com.github.jragcer.securitycore.api.biometric.BiometricResult
import com.github.jragcer.securitycore.api.biometric.BiometricStatus

/**
 * Internal implementation of [BiometricAuthManager] using AndroidX BiometricPrompt.
 */
internal class BiometricAuthManagerImpl (
    private val context: Context
) : BiometricAuthManager{

    private val biometricManager = BiometricManager.from(context)

    /**
     * Determines whether biometric authentication can proceed given current hardware and enrollment state.
     */
    override fun canAuthenticate(allowDeviceCredential: Boolean): BiometricStatus {
        val authenticators = if (allowDeviceCredential) {
            BIOMETRIC_STRONG or DEVICE_CREDENTIAL
        }else{
            BIOMETRIC_STRONG
        }

        return when (biometricManager.canAuthenticate(authenticators)){
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.Ready
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NotEnrolled
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HardwareUnavailable
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NoHardware
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricStatus.SecurityUpdateRequired
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> BiometricStatus.Unsupported
            else -> BiometricStatus.HardwareUnavailable
        }
    }

    /**
     * Launches the biometric prompt without an associated cryptographic object.
     */
    override fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String?,
        negativeButtonText: String,
        allowDeviceCredential: Boolean,
        onResult: (BiometricResult) -> Unit
    ) {
        val promptInfo = createPromptInfo(
            title = title,
            subtitle = subtitle,
            negativeButtonText = negativeButtonText,
            allowDeviceCredential = allowDeviceCredential
        )

        val prompt = buildBiometricPrompt(activity, onResult)
        prompt.authenticate(promptInfo)
    }

    /**
     * Launches the biometric prompt bound to a hardware-backed [BiometricPrompt.CryptoObject].
     */
    override fun authenticateWithCrypto(
        activity: FragmentActivity,
        cryptoObject: BiometricPrompt.CryptoObject,
        title: String,
        subtitle: String?,
        negativeButtonText: String,
        allowDeviceCredential: Boolean,
        onResult: (BiometricResult) -> Unit
    ) {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .apply { subtitle?.let { setSubtitle(it) } }
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BIOMETRIC_STRONG)
            .build()

        val prompt = buildBiometricPrompt(activity, onResult)
        prompt.authenticate(promptInfo)
    }

    /**
     * Configures the [BiometricPrompt.PromptInfo] according to credential fallback preferences.
     */
    private fun createPromptInfo(
        title: String,
        subtitle: String?,
        negativeButtonText: String,
        allowDeviceCredential: Boolean
    ): BiometricPrompt.PromptInfo {
        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .apply { subtitle?.let { setSubtitle(it) } }

        if(allowDeviceCredential){
            builder.setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        }else{
            builder.setAllowedAuthenticators(BIOMETRIC_STRONG)
            builder.setNegativeButtonText(negativeButtonText)
        }

        return builder.build()
    }

    /**
     * Instantiates a [BiometricPrompt] using the main thread executor and maps callbacks to [BiometricResult].
     */
    private fun buildBiometricPrompt(
        activity: FragmentActivity,
        onResult: (BiometricResult) -> Unit
    ) : BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback(){
             override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onResult(BiometricResult.Success(result.cryptoObject))
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onResult(BiometricResult.Error(errorCode, errString.toString()))
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onResult(BiometricResult.Failed)
            }
        }

        return BiometricPrompt(activity, executor, callback)
    }
}