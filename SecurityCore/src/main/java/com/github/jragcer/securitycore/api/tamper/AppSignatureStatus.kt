package com.github.jragcer.securitycore.api.tamper

/**
 * Diagnostic result of application package signature and origin verification.
 *
 * @property isSignatureValid True if the active signing certificate matches the expected hash.
 * @property isInstallerTrusted True if the app was installed from an authorized store source.
 * @property currentSignatureHash The SHA-256 fingerprint of the current signing certificate (hex-encoded).
 * @property installerPackageName The name of the installer package of the application.
 */
data class AppSignatureStatus (
    val isSignatureValid: Boolean,
    val isInstallerTrusted: Boolean,
    val currentSignatureHash: String,
    val installerPackageName: String?
){
    val isGenuine: Boolean
        get() = isSignatureValid && isInstallerTrusted
}