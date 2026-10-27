package com.github.jragcer.securitycore.api.crypto

/**
 * Base exception for cryptographic operations within SecurityCore.
 */
sealed class CryptoException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /**
     * Thrown when an error occurs while encrypting data or streaming output.
     */
    class EncryptionException(message: String, cause: Throwable? = null) : CryptoException(message, cause)

    /**
     * Thrown when an error occurs during decryption, including authentication tag failures
     * or corrupted payload structures.
     */
    class DecryptionException(message: String, cause: Throwable? = null) : CryptoException(message, cause)

    /**
     * Thrown when the underlying AndroidKeyStore cannot be accessed, initialized,
     * or when key generation fails.
     */
    class KeyStoreAccessException(message: String, cause: Throwable? = null) : CryptoException(message, cause)
}