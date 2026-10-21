package com.github.jragcer.securitycore.api.biometric

import androidx.biometric.BiometricPrompt

/**
 *  Result states for a biometric authentication operation.
 */
sealed interface BiometricResult {

    /**
     * Authentication was successful.
     * @property cryptoObject Optional [BiometricPrompt.CryptoObject] associated with the authentication.
     */
    data class Success(
        val cryptoObject: BiometricPrompt.CryptoObject? = null
    ) : BiometricResult

    /**
     * An unrecoverable error has occurred. (e.g., hardware not available, user canceled, too many attempts)
     * @property errorCode System error code from [BiometricPrompt].
     * @property errorMessage Localized explanation of the error.
     */
    data class Error(
        val errorCode: Int,
        val errorMessage: String
    ) : BiometricResult

    /**
     * Biometric rejected (e.g.,unrecognized fingerprint or face)
     */
    data object Failed : BiometricResult
}

/**
 * Hardware and enrollment status of biometric authentication on the device.
 */
sealed interface BiometricStatus {
    data object Ready : BiometricStatus
    data object NotEnrolled: BiometricStatus
    data object HardwareUnavailable : BiometricStatus
    data object NoHardware : BiometricStatus
    data object SecurityUpdateRequired : BiometricStatus
    data object Unsupported : BiometricStatus
}
