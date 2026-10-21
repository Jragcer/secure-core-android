package com.github.jragcer.securitycore.api.biometric

import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity

/**
 *  Public interface for managing biometric authentication prompts and device status checks.
 */
interface BiometricAuthManager {

    /**
     *  Checks if biometric authentication can be performed on the current device.
     *
     *  @param allowDeviceCredential If true, allows PIN, pattern, or password fallback.
     */
    fun canAuthenticate(allowDeviceCredential: Boolean = false): BiometricStatus

    /**
     *  Prompts the user for biometric authentication
     *
     *  @param activity Hosting [FragmentActivity] required by AndroidX BiometricPrompt.
     *  @param title Title displayed on the biometric dialog.
     *  @param subtitle Optional subtitle displayed on the biometric dialog.
     *  @param negativeButtonText Text displayed on the negative button (ignored if allowDeviceCredential is true).
     *  @param allowDeviceCredential If true, allows PIN, pattern, or password fallback.
     *  @param onResult Callback delivering the final [BiometricResult].
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String? = null,
        negativeButtonText: String = "Cancel",
        allowDeviceCredential: Boolean = false,
        onResult: (BiometricResult) -> Unit
    )

    /**
     *  Prompts the user for biometric authentication tied to a cryptographic operation
     *
     *  @param title Title displayed on the biometric dialog.
     *  @param cryptoObject Cryptographic object associated to authenticate (e.g., Keystore-backed Cipher).
     *  @param subtitle Optional subtitle displayed on the biometric dialog.
     *  @param negativeButtonText Text displayed on the negative button (ignored if allowDeviceCredential is true).
     *  @param allowDeviceCredential If true, allows PIN, pattern, or password fallback.
     *  @param onResult Callback delivering the final [BiometricResult].
     */
    fun authenticateWithCrypto(
        activity: FragmentActivity,
        cryptoObject: BiometricPrompt.CryptoObject,
        title: String,
        subtitle: String? = null,
        negativeButtonText: String = "Cancel",
        allowDeviceCredential: Boolean = false,
        onResult: (BiometricResult) -> Unit
    )
}