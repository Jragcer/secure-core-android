package com.github.jragcer.securitycore.api.tamper

/**
 * Public interface providing runtime validation against APK tampering,
 * repackaging, and unauthorized sideloading.
 */
interface TamperProtectionManager {
    /**
     * Extracts and computes the SHA-256 fingerprint of the primary signing certificate
     * currently validating the running APK.
     *
     * @return Uppercase hex string without separators representing the SHA-256 signature hash,
     *          or an empty string if retrieval fails or certificates are unavailable.
     */
    fun getAppSignatureHash() : String

    /**
     * Evaluates whether the currently installed APK's signing certificate matches the expected
     * genuine fingerprint.
     *
     * The comparison normalizes colons and whitespace, and performs a case-insensitive evaluation.
     *
     * @param expectedHash The trusted SHA-256 fingerprint (with or without colons).
     * @return True if the signature matches the expected hash; false if it differs, or if either
     *         the active or expected hash is blank.
     */
    fun verifySignature(expectedHash: String) : Boolean

    /**
     * Determines whether the package installer corresponds to an authorized source (e.g. Google Play).
     *
     * @param trustedInstallers Set of allowed package names representing official store sources.
     *                          Defaults to Google Play Store ("com.android.vending").
     * @return True if the installer package is non-null and present within [trustedInstallers].
     */
    fun isInstalledFromTrustedStore(trustedInstallers: Set<String> = setOf("com.android.vending")) : Boolean

    /**
     * Gathers a consolidated [AppSignatureStatus] evaluating both cryptographic certificate validity
     * and store origin.
     *
     * @param expectedHash The expected genuine SHA-256 signing fingerprint.
     * @param trustedInstallers Set of allowed installer package names.
     * @return An [AppSignatureStatus] reflecting the combined diagnostic outcome.
     */
    fun getSignatureStatus(expectedHash: String,
                           trustedInstallers: Set<String> = setOf("com.android.vending")) : AppSignatureStatus
}