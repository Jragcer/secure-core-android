package com.github.jragcer.securitycore.api.crypto

/**
 * Supported asymmetric cryptographic algorithms and key specifications.
 */
sealed class AsymmetricAlgorithm {
    /**
     * RSA key pair specification supporting encryption, decryption, and digital signatures.
     *
     * @property keySize Key length in bits (e.g., 2048, 3072, 4096).
     * @property padding Cipher transformation padding (defaults to OAEP with SHA-256 and MGF1).
     */
    data class RSA(
        val keySize: Int = 2048,
        val padding: String = "OAEPWithSHA-256AndMGF1Padding"
    ) : AsymmetricAlgorithm()

    /**
     * Elliptic Curve (EC) specification optimized for fast digital signatures (ECDSA)
     * and compact signature payloads using the NIST P-256 / secp256r1 curve.
     *
     * @property keySize Key length in bits (defaults to 256 bits).
     */
    data class EC(
        val keySize: Int = 256
    ) : AsymmetricAlgorithm()
}