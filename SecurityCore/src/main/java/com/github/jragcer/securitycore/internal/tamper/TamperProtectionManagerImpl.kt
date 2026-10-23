package com.github.jragcer.securitycore.internal.tamper

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import com.github.jragcer.securitycore.api.tamper.AppSignatureStatus
import com.github.jragcer.securitycore.api.tamper.TamperProtectionManager
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * Internal implementation of [TamperProtectionManager] providing runtime inspection
 * mechanisms to guard against application tampering, repackaging, and unauthorized sideloading.
 *
 * This implementation queries the Android [PackageManager] to inspect raw X.509 signing certificates
 * across different Android OS versions:
 * - On Android 9.0 (API 28, Pie) and above, it utilizes [android.content.pm.SigningInfo] to inspect
 *   the certificate history and multi-signer configurations.
 * - On earlier versions, it falls back to the legacy [PackageManager.GET_SIGNATURES] flag.
 *
 * Installation origin checks are resolved using [PackageManager.getInstallSourceInfo] on Android 11+
 * (API 30) or the legacy [PackageManager.getInstallerPackageName] on earlier versions.
 *
 * @param context Application or activity context used to access package metadata.
 */
internal class TamperProtectionManagerImpl (
    private val context: Context
): TamperProtectionManager {
    private val appContext: Context = context.applicationContext

    /**
     * Extracts and computes the SHA-256 fingerprint of the primary signing certificate
     * currently validating the running APK.
     *
     * @return Uppercase hex string without separators representing the SHA-256 signature hash,
     *          or an empty string if retrieval fails or certificates are unavailable.
     */
    override fun getAppSignatureHash(): String {
        return try {
            val signatures = getSignatures()
            if(signatures.isEmpty()) return ""
            computeSha256(signatures[0].toByteArray())
        }catch (_: Exception){
            ""
        }
    }

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
    override fun verifySignature(expectedHash: String): Boolean {
        val currentHash = getAppSignatureHash()
        if(currentHash.isEmpty() || expectedHash.isBlank()) return false
        return currentHash.equals(expectedHash.replace(":", "").trim(), ignoreCase = true)
    }

    /**
     * Determines whether the package installer corresponds to an authorized source (e.g. Google Play).
     *
     * @param trustedInstallers Set of allowed package names representing official store sources.
     *                          Defaults to Google Play Store ("com.android.vending").
     * @return True if the installer package is non-null and present within [trustedInstallers].
     */
    override fun isInstalledFromTrustedStore(trustedInstallers: Set<String>): Boolean {
        val installer = getInstallerPackageName() ?: return false
        return trustedInstallers.contains(installer)
    }

    /**
     * Gathers a consolidated [AppSignatureStatus] evaluating both cryptographic certificate validity
     * and store origin.
     *
     * @param expectedHash The expected genuine SHA-256 signing fingerprint.
     * @param trustedInstallers Set of allowed installer package names.
     * @return An [AppSignatureStatus] reflecting the combined diagnostic outcome.
     */
    override fun getSignatureStatus(
        expectedHash: String,
        trustedInstallers: Set<String>
    ): AppSignatureStatus {
        val currentHash = getAppSignatureHash()
        val installer = getInstallerPackageName()
        val signatureValid = currentHash.isNotEmpty() &&
            currentHash.equals(expectedHash.replace(":", "").trim(), ignoreCase = true)
        val installerTrusted = installer != null && trustedInstallers.contains(installer)

        return AppSignatureStatus(
            isSignatureValid = signatureValid,
            isInstallerTrusted = installerTrusted,
            currentSignatureHash = currentHash,
            installerPackageName = installer
        )
    }

    /**
     * Retrieves the array of raw signing certificates associated with the current package.
     *
     * Handles API differences across platform versions safely.
     *
     * @return An array of [Signature] objects, or an empty array if querying fails.
     */
    @Suppress("DEPRECATION")
    @SuppressLint("PackageManagerGetSignatures")
    private fun getSignatures(): Array<Signature> {
        val packageManager = appContext.packageManager
        val packageName = appContext.packageName

        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
            val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            val signingInfo = packageInfo.signingInfo ?: return emptyArray()

            if(signingInfo.hasMultipleSigners()){
                signingInfo.signingCertificateHistory
            }else{
                signingInfo.signingCertificateHistory
            }
        }else {
            val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            packageInfo.signatures ?: emptyArray()
        }
    }

    /**
     * Determines the package name of the installer that deployed this application.
     *
     * Employs [PackageManager.getInstallSourceInfo] on Android 11+ (API 30) and falls back
     * to [PackageManager.getInstallerPackageName] on earlier versions.
     *
     * @return The installer package name (e.g. "com.android.vending"), or null if sideloaded or unavailable.
     */
    @Suppress("DEPRECATION")
    private fun getInstallerPackageName(): String? {
        val packageManager = appContext.packageManager
        val packageName = appContext.packageName

        return try {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
                packageManager.getInstallSourceInfo(packageName).installingPackageName
            }else{
                packageManager.getInstallerPackageName(packageName)
            }
        }catch (_: Exception){
            null
        }
    }

    /**
     * Computes the SHA-256 digest over the provided byte sequence and encodes it as uppercase hexadecimal.
     *
     * @param bytes Raw byte array to hash (typically X.509 DER-encoded certificate bytes).
     * @return Uppercase hexadecimal digest string.
     */
    private fun computeSha256(bytes: ByteArray): String{
        return try{
            val md= MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            digest.joinToString("") { "%02X".format(it) }
        }catch (_: NoSuchAlgorithmException){
            ""
        }
    }
}