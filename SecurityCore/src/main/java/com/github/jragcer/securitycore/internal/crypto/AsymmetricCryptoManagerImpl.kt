package com.github.jragcer.securitycore.internal.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.github.jragcer.securitycore.api.crypto.AsymmetricAlgorithm
import com.github.jragcer.securitycore.api.crypto.AsymmetricCryptoManager
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.Signature
import java.security.SignatureException
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.MGF1ParameterSpec
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource

/**
 * [AsymmetricCryptoManager] backed by the AndroidKeyStore.
 *
 * The key pair is generated inside the Keystore and the private key never leaves it. Whether that
 * storage is hardware-backed (TEE) depends on the device. StrongBox is NOT requested.
 *
 * Behaviour worth knowing:
 * - One key pair per alias is used for every operation (RSA: encrypt, decrypt, sign, verify;
 *   EC: sign, verify). To keep purposes separate, create one manager per alias.
 * - The constructor generates the key pair when the alias is empty. RSA generation can take
 *   noticeable time, so create instances off the main thread.
 * - If the alias already holds a key of a different type or size, construction fails with an
 *   [IllegalStateException] instead of failing later on first use.
 * - Keys are not bound to user authentication (no biometric/PIN is required to use them).
 * - After [deleteKey] this instance can no longer be used; create a new one to get a new key pair.
 * - Failures are signalled with exceptions, not with [Result].
 *
 * @property keyAlias The unique alias used to store and retrieve the key pair in the Keystore.
 * @property algorithm The asymmetric algorithm configuration to use.
 */
internal class AsymmetricCryptoManagerImpl(
    private val keyAlias: String,
    private val algorithm: AsymmetricAlgorithm = AsymmetricAlgorithm.RSA()
) : AsymmetricCryptoManager {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val RSA_TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
        private const val RSA_SIGNATURE_ALGORITHM = "SHA256withRSA"
        private const val EC_SIGNATURE_ALGORITHM = "SHA256withECDSA"
        private const val EC_CURVE = "secp256r1"
        private const val EC_FIELD_SIZE_BITS = 256
        private const val OAEP_HASH_BYTES = 32 // SHA-256

        /**
         * Process-wide lock. Key creation, lookup and deletion are serialised so two instances
         * using the same alias cannot generate two different key pairs.
         */
        private val KEY_LOCK = Any()

        // Canonical OAEP parameters for the AndroidKeyStore: OAEP digest SHA-256, MGF1 digest SHA-1.
        private val OAEP_SPEC = OAEPParameterSpec(
            "SHA-256",
            "MGF1",
            MGF1ParameterSpec.SHA1,
            PSource.PSpecified.DEFAULT
        )
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    private val signatureAlgorithm: String = when (algorithm) {
        is AsymmetricAlgorithm.RSA -> RSA_SIGNATURE_ALGORITHM
        is AsymmetricAlgorithm.EC -> EC_SIGNATURE_ALGORITHM
    }

    init {
        require(keyAlias.isNotBlank()) { "keyAlias must not be blank" }
        ensureKeyPairExists()
    }

    /**
     * Returns the public key associated with the configured alias.
     *
     * @throws IllegalStateException if the key pair no longer exists (for example after [deleteKey]).
     */
    override fun getPublicKey(): PublicKey {
        return synchronized(KEY_LOCK) {
            keyStore.getCertificate(keyAlias)?.publicKey
                ?: throw IllegalStateException(
                    "No key pair found for alias '$keyAlias'. It may have been deleted with deleteKey(); " +
                            "create a new manager instance to generate a new one."
                )
        }
    }

    /**
     * Exports the public key in standard X.509 format (SubjectPublicKeyInfo / DER).
     * Suitable for sending to a backend or for Base64/PEM encoding.
     */
    override fun getPublicKeyEncoded(): ByteArray {
        return getPublicKey().encoded
    }

    /**
     * Encrypts raw bytes with the public key using RSA-OAEP (SHA-256, MGF1-SHA1).
     *
     * RSA can only encrypt a small payload: about 190 bytes for a 2048-bit key. For larger data,
     * encrypt the data with a symmetric key and use RSA only for that key (hybrid encryption).
     * Any other party encrypting for this key must use the same OAEP parameters.
     *
     * @throws IllegalStateException if this instance was created with an EC algorithm.
     * @throws IllegalArgumentException if [data] is larger than the RSA-OAEP limit for this key.
     */
    override fun encrypt(data: ByteArray): ByteArray {
        check(algorithm is AsymmetricAlgorithm.RSA) {
            "Direct asymmetric encryption is only supported for RSA keys. " +
                    "Use hybrid encryption or ECDH for EC keys."
        }

        val publicKey = getPublicKey()
        val maxBytes = maxOaepPayloadBytes(publicKey)
        require(data.size <= maxBytes) {
            "Payload of ${data.size} bytes exceeds the RSA-OAEP limit of $maxBytes bytes for this key. " +
                    "Use hybrid encryption for larger data."
        }

        val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, OAEP_SPEC)
        return cipher.doFinal(data)
    }

    /**
     * Decrypts ciphertext with the private key held in the Keystore.
     *
     * @throws IllegalStateException if this instance was created with an EC algorithm.
     * @throws java.security.GeneralSecurityException if the ciphertext is invalid or was produced
     *   with different OAEP parameters.
     */
    override fun decrypt(encryptedData: ByteArray): ByteArray {
        check(algorithm is AsymmetricAlgorithm.RSA) {
            "Direct asymmetric decryption is only supported for RSA keys."
        }

        val cipher = Cipher.getInstance(RSA_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getPrivateKey(), OAEP_SPEC)
        return cipher.doFinal(encryptedData)
    }

    /**
     * Signs [data] with the private key held in the Keystore.
     *
     * RSA uses SHA256withRSA (PKCS#1 v1.5); EC uses SHA256withECDSA (DER-encoded signature).
     */
    override fun sign(data: ByteArray): ByteArray {
        val signer = Signature.getInstance(signatureAlgorithm)
        signer.initSign(getPrivateKey())
        signer.update(data)
        return signer.sign()
    }

    /**
     * Verifies [signature] over [data].
     *
     * Returns `false` for a signature that is invalid or malformed. Throws only for misuse, such as
     * passing a public key of the wrong type.
     *
     * @param publicKey Optional external public key. If null, this manager's own public key is used.
     * @return `true` if the signature is valid; `false` otherwise.
     */
    override fun verify(data: ByteArray, signature: ByteArray, publicKey: PublicKey?): Boolean {
        val verifier = Signature.getInstance(signatureAlgorithm)
        verifier.initVerify(publicKey ?: getPublicKey())
        verifier.update(data)
        return try {
            verifier.verify(signature)
        } catch (e: SignatureException) {
            // A malformed or wrong-length signature is simply "not valid".
            false
        }
    }

    /**
     * Permanently deletes the key pair from the Keystore. This instance is unusable afterwards.
     *
     * @return `true` if a key entry existed and was removed.
     */
    override fun deleteKey(): Boolean {
        return synchronized(KEY_LOCK) {
            if (keyStore.containsAlias(keyAlias)) {
                keyStore.deleteEntry(keyAlias)
                true
            } else {
                false
            }
        }
    }

    private fun getPrivateKey(): PrivateKey {
        return synchronized(KEY_LOCK) {
            keyStore.getKey(keyAlias, null) as? PrivateKey
                ?: throw IllegalStateException(
                    "Private key not found for alias '$keyAlias'. It may have been deleted with " +
                            "deleteKey(); create a new manager instance to generate a new one."
                )
        }
    }

    /**
     * Creates the key pair if the alias is empty, or validates that the existing entry matches the
     * requested algorithm and size. Serialised across all instances in the process.
     */
    private fun ensureKeyPairExists() {
        synchronized(KEY_LOCK) {
            if (keyStore.containsAlias(keyAlias)) {
                validateExistingKey()
            } else {
                generateKeyPair()
            }
        }
    }

    private fun validateExistingKey() {
        val publicKey = keyStore.getCertificate(keyAlias)?.publicKey
            ?: throw IllegalStateException(
                "Alias '$keyAlias' already exists but does not hold an asymmetric key pair " +
                        "(it may be a symmetric key). Use a different alias."
            )

        when (algorithm) {
            is AsymmetricAlgorithm.RSA -> {
                val rsaKey = publicKey as? RSAPublicKey
                    ?: throw IllegalStateException(
                        "Alias '$keyAlias' already holds a ${publicKey.algorithm} key but RSA was " +
                                "requested. Use a different alias or delete the existing key first."
                    )
                val actualBits = rsaKey.modulus.bitLength()
                check(actualBits == algorithm.keySize) {
                    "Alias '$keyAlias' already holds an RSA-$actualBits key but RSA-${algorithm.keySize} " +
                            "was requested. Use a different alias or delete the existing key first."
                }
            }
            is AsymmetricAlgorithm.EC -> {
                val ecKey = publicKey as? ECPublicKey
                    ?: throw IllegalStateException(
                        "Alias '$keyAlias' already holds a ${publicKey.algorithm} key but EC was " +
                                "requested. Use a different alias or delete the existing key first."
                    )
                val fieldBits = ecKey.params.curve.field.fieldSize
                check(fieldBits == EC_FIELD_SIZE_BITS) {
                    "Alias '$keyAlias' holds an EC key on a $fieldBits-bit curve but P-256 was " +
                            "requested. Use a different alias or delete the existing key first."
                }
            }
        }
    }

    private fun maxOaepPayloadBytes(publicKey: PublicKey): Int {
        val modulusBytes = ((publicKey as RSAPublicKey).modulus.bitLength() + 7) / 8
        return modulusBytes - 2 * OAEP_HASH_BYTES - 2
    }

    private fun generateKeyPair() {
        val keyPairGenerator = KeyPairGenerator.getInstance(
            when (algorithm) {
                is AsymmetricAlgorithm.RSA -> KeyProperties.KEY_ALGORITHM_RSA
                is AsymmetricAlgorithm.EC -> KeyProperties.KEY_ALGORITHM_EC
            },
            ANDROID_KEYSTORE
        )

        val spec = when (algorithm) {
            is AsymmetricAlgorithm.RSA -> {
                KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or
                            KeyProperties.PURPOSE_DECRYPT or
                            KeyProperties.PURPOSE_SIGN or
                            KeyProperties.PURPOSE_VERIFY
                )
                    .setKeySize(algorithm.keySize)
                    // SHA-1 is required because the Keystore uses SHA-1 for the MGF1 function in OAEP.
                    // It also makes SHA-1 signatures possible for this key, but this class only
                    // ever signs with SHA-256.
                    .setDigests(KeyProperties.DIGEST_SHA1, KeyProperties.DIGEST_SHA256)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                    // PKCS#1 v1.5 for broad backend compatibility. RSA-PSS is the stronger choice
                    // if both ends support it.
                    .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                    .build()
            }
            is AsymmetricAlgorithm.EC -> {
                KeyGenParameterSpec.Builder(
                    keyAlias,
                    KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
                )
                    .setAlgorithmParameterSpec(ECGenParameterSpec(EC_CURVE))
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .build()
            }
        }

        keyPairGenerator.initialize(spec)
        keyPairGenerator.generateKeyPair()
    }
}
