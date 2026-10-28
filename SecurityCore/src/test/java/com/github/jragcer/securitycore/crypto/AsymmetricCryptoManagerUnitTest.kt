package com.github.jragcer.securitycore.crypto

import com.github.jragcer.securitycore.api.crypto.AsymmetricAlgorithm
import com.github.jragcer.securitycore.api.crypto.AsymmetricCryptoManager
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

class AsymmetricCryptoManagerUnitTest {
    // =========================================================================
    // 1. Algorithm Configuration & Model Invariants
    // =========================================================================

    @Test
    fun testRsaDefaultParametersMatchEnterpriseStandards() {
        val rsa = AsymmetricAlgorithm.RSA()
        assertEquals(2048, rsa.keySize)
        assertEquals("OAEPWithSHA-256AndMGF1Padding", rsa.padding)
    }

    @Test
    fun testRsaCustomKeySizesArePreserved() {
        val keySizes = listOf(2048, 3072, 4096)
        keySizes.forEach { size ->
            val rsa = AsymmetricAlgorithm.RSA(keySize = size)
            assertEquals(size, rsa.keySize)
        }
    }

    @Test
    fun testEcConfigurationParameters() {
        val ec = AsymmetricAlgorithm.EC()
        assertEquals(256, ec.keySize)
        assertTrue(ec is AsymmetricAlgorithm)
    }

    @Test
    fun testAlgorithmEqualityAndCopyContracts() {
        val rsa1 = AsymmetricAlgorithm.RSA(keySize = 3072)
        val rsa2 = AsymmetricAlgorithm.RSA(keySize = 3072)
        val rsa3 = AsymmetricAlgorithm.RSA(keySize = 2048)

        assertEquals(rsa1, rsa2)
        assertEquals(rsa1.hashCode(), rsa2.hashCode())
        assertFalse(rsa1 == rsa3)

        val modified = rsa1.copy(keySize = 4096)
        assertEquals(4096, modified.keySize)
        assertNotSame(rsa1, modified)
    }

    // =========================================================================
    // 2. Contract Sad Paths & State Invariants (Fake Implementation)
    // =========================================================================

    @Test
    fun testEcDirectEncryptionThrowsIllegalStateException() {
        val fakeEcManager = createFakeAsymmetricManager(isEc = true)

        try {
            fakeEcManager.encrypt("Some sensitive data".toByteArray(Charsets.UTF_8))
            fail("Expected IllegalStateException when calling encrypt() on an EC instance")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("only supported for RSA", ignoreCase = true) == true)
        }
    }

    @Test
    fun testEcDirectDecryptionThrowsIllegalStateException() {
        val fakeEcManager = createFakeAsymmetricManager(isEc = true)

        try {
            fakeEcManager.decrypt(ByteArray(64) { 0x01 })
            fail("Expected IllegalStateException when calling decrypt() on an EC instance")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("only supported for RSA", ignoreCase = true) == true)
        }
    }

    @Test
    fun testEmptyPayloadSigningBehavior() {
        val fakeRsaManager = createFakeAsymmetricManager(isEc = false)
        val emptyPayload = ByteArray(0)

        // Signing an empty byte array should succeed and produce a valid signature format
        val signature = fakeRsaManager.sign(emptyPayload)
        assertNotNull(signature)
        assertTrue("Signature of empty payload must not be empty", signature.isNotEmpty())

        val isValid = fakeRsaManager.verify(emptyPayload, signature)
        assertTrue("Signature verification of empty payload must succeed", isValid)
    }

    @Test
    fun testCorruptedSignatureFailsVerificationGracefully() {
        val fakeRsaManager = createFakeAsymmetricManager(isEc = false)
        val data = "Financial Wire Transfer".toByteArray(Charsets.UTF_8)
        val validSignature = fakeRsaManager.sign(data)

        // Flip bytes in the signature to simulate corruption / bit rot
        val corruptedSignature = validSignature.clone()
        corruptedSignature[0] = (corruptedSignature[0].toInt() xor 0xFF).toByte()

        val isValid = fakeRsaManager.verify(data, corruptedSignature)
        assertFalse("Verification of corrupted signature must return false", isValid)
    }

    @Test
    fun testTruncatedSignatureFailsVerificationGracefully() {
        val fakeRsaManager = createFakeAsymmetricManager(isEc = false)
        val data = "Security Clearance Level 5".toByteArray(Charsets.UTF_8)
        val validSignature = fakeRsaManager.sign(data)

        // Truncate signature to half its length
        val truncatedSignature = validSignature.copyOf(validSignature.size / 2)

        val isValid = fakeRsaManager.verify(data, truncatedSignature)
        assertFalse("Verification of truncated signature must return false", isValid)
    }

    // =========================================================================
    // 3. X.509 Key Encoding & Reconstruction Standards
    // =========================================================================

    @Test
    fun testExportedPublicKeyCanBeReconstructedViaStandardX509Spec() {
        // Generate a standard JVM RSA key to validate standard encoding compliance
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val keyPair = kpg.generateKeyPair()

        val encodedBytes = keyPair.public.encoded
        assertNotNull(encodedBytes)
        assertTrue("X.509 encoded public key must have non-zero length", encodedBytes.isNotEmpty())

        // Reconstruct public key from byte array using standard X509EncodedKeySpec
        val keyFactory = KeyFactory.getInstance("RSA")
        val reconstructedKey = keyFactory.generatePublic(X509EncodedKeySpec(encodedBytes))

        assertNotNull(reconstructedKey)
        assertEquals("RSA", reconstructedKey.algorithm)
        assertEquals("X.509", reconstructedKey.format)
        assertArrayEquals(keyPair.public.encoded, reconstructedKey.encoded)
    }

    @Test
    fun testExportedEcPublicKeyCanBeReconstructedViaStandardX509Spec() {
        val kpg = KeyPairGenerator.getInstance("EC")
        kpg.initialize(256)
        val keyPair = kpg.generateKeyPair()

        val encodedBytes = keyPair.public.encoded
        assertNotNull(encodedBytes)

        val keyFactory = KeyFactory.getInstance("EC")
        val reconstructedKey = keyFactory.generatePublic(X509EncodedKeySpec(encodedBytes))

        assertNotNull(reconstructedKey)
        assertEquals("EC", reconstructedKey.algorithm)
        assertEquals("X.509", reconstructedKey.format)
        assertArrayEquals(keyPair.public.encoded, reconstructedKey.encoded)
    }

    // =========================================================================
    // Test Double Helper for JVM Testing without AndroidKeyStore
    // =========================================================================

    private fun createFakeAsymmetricManager(isEc: Boolean): AsymmetricCryptoManager {
        val keyPair = if (isEc) {
            KeyPairGenerator.getInstance("EC").apply { initialize(256) }.generateKeyPair()
        } else {
            KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        }

        return object : AsymmetricCryptoManager {
            override fun getPublicKey(): PublicKey = keyPair.public
            override fun getPublicKeyEncoded(): ByteArray = keyPair.public.encoded

            override fun encrypt(data: ByteArray): ByteArray {
                check(!isEc) { "Direct asymmetric encryption is only supported for RSA keys." }
                return ByteArray(128) { 0x42 }
            }

            override fun decrypt(encryptedData: ByteArray): ByteArray {
                check(!isEc) { "Direct asymmetric decryption is only supported for RSA keys." }
                return "Decrypted".toByteArray()
            }

            override fun sign(data: ByteArray): ByteArray {
                val algorithm = if (isEc) "SHA256withECDSA" else "SHA256withRSA"
                val signer = Signature.getInstance(algorithm)
                signer.initSign(keyPair.private)
                signer.update(data)
                return signer.sign()
            }

            override fun verify(data: ByteArray, signature: ByteArray, publicKey: PublicKey?): Boolean {
                return try {
                    val algorithm = if (isEc) "SHA256withECDSA" else "SHA256withRSA"
                    val verifier = Signature.getInstance(algorithm)
                    verifier.initVerify(publicKey ?: keyPair.public)
                    verifier.update(data)
                    verifier.verify(signature)
                } catch (e: Exception) {
                    false
                }
            }

            override fun deleteKey(): Boolean = true
        }
    }
}