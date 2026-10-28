package com.github.jragcer.securitycore.api.crypto

import java.security.PublicKey

/**
 * Public interface for asymmetric cryptography and digital signature operations
 * backed by hardware security modules (AndroidKeyStore / TEE / StrongBox).
 */
interface AsymmetricCryptoManager {
    /**
     * Retrieves the public key associated with the configured alias.
     *
     * @return The public key.
     */
    fun getPublicKey(): PublicKey

    /**
     * Exports the public key encoded in standard X.509 format (SubjectPublicKeyInfo).
     * Suitable for backend transmission, persistence, or Base64/PEM encoding.
     *
     * @return Encoded public key bytes.
     */
    fun getPublicKeyEncoded(): ByteArray

    /**
     * Encrypts arbitrary raw bytes using the public key.
     * Note: Typically supported for RSA algorithms; EC requires hybrid or key-exchange schemes.
     *
     * @param data Plaintext data to encrypt.
     * @return Encrypted ciphertext bytes.
     */
    fun encrypt(data: ByteArray): ByteArray

    /**
     * Decrypts ciphertext bytes using the private key secured within the hardware KeyStore.
     *
     * @param encryptedData Ciphertext data to decrypt.
     * @return Original decrypted plaintext bytes.
     */
    fun decrypt(encryptedData: ByteArray): ByteArray

    /**
     * Digitally signs a data payload using the KeyStore-backed private key.
     *
     * @param data Payload to sign.
     * @return Computed digital signature bytes.
     */
    fun sign(data: ByteArray): ByteArray

    /**
     * Verifies the integrity and authenticity of a payload against a digital signature.
     *
     * @param data Original data payload.
     * @param signature Digital signature bytes to verify.
     * @param publicKey Optional external public key. If null, the local key pair's public key is used.
     * @return True if the signature is valid; false otherwise.
     */
    fun verify(data: ByteArray, signature: ByteArray, publicKey: PublicKey? = null): Boolean

    /**
     * Deletes the asymmetric key pair permanently from the hardware KeyStore.
     *
     * @return True if the key entry was successfully removed.
     */
    fun deleteKey(): Boolean
}