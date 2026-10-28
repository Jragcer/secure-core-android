package com.github.jragcer.securitycore.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.jragcer.securitycore.SecureCore
import com.github.jragcer.securitycore.api.crypto.AsymmetricAlgorithm
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AsymmetricCryptoManagerInstrumentedTest {
    private val rsaAlias = "test_asymmetric_rsa_alias"
    private val ecAlias = "test_asymmetric_ec_alias"

    @Before
    fun setUp() {
        runCatching {
            val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            keyStore.deleteEntry(rsaAlias)
            keyStore.deleteEntry(ecAlias)
        }
    }

    @After
    fun tearDown() {
        // Clean up KeyStore entries after each test to ensure test isolation
        runCatching { SecureCore.createAsymmetricCryptoManager(rsaAlias).deleteKey() }
        runCatching {
            SecureCore.createAsymmetricCryptoManager(
                ecAlias,
                AsymmetricAlgorithm.EC()
            ).deleteKey()
        }
    }

    @Test
    fun testRsaKeyGenerationAndPublicKeyExport() {
        val manager = SecureCore.createAsymmetricCryptoManager(
            alias = rsaAlias,
            algorithm = AsymmetricAlgorithm.RSA(keySize = 2048)
        )

        val publicKey = manager.getPublicKey()
        val encodedKey = manager.getPublicKeyEncoded()

        assertNotNull(publicKey)
        assertEquals("RSA", publicKey.algorithm)
        assertTrue(encodedKey.isNotEmpty())
    }

    @Test
    fun testRsaEncryptionAndDecryption() {
        val manager = SecureCore.createAsymmetricCryptoManager(
            alias = rsaAlias,
            algorithm = AsymmetricAlgorithm.RSA(keySize = 2048)
        )

        val plaintext = "Enterprise Confidential Payload".toByteArray(Charsets.UTF_8)

        val ciphertext = manager.encrypt(plaintext)
        assertNotNull(ciphertext)
        assertFalse(plaintext.contentEquals(ciphertext))

        val decrypted = manager.decrypt(ciphertext)
        assertArrayEquals(plaintext, decrypted)
        assertEquals("Enterprise Confidential Payload", String(decrypted, Charsets.UTF_8))
    }

    @Test
    fun testRsaDigitalSignatureAndVerification() {
        val manager = SecureCore.createAsymmetricCryptoManager(
            alias = rsaAlias,
            algorithm = AsymmetricAlgorithm.RSA(keySize = 2048)
        )

        val payload = "Transaction: Transfer $10,000 to Account B".toByteArray(Charsets.UTF_8)

        val signature = manager.sign(payload)
        assertNotNull(signature)
        assertTrue(signature.isNotEmpty())

        val isValid = manager.verify(payload, signature)
        assertTrue("Signature should be valid for unaltered payload", isValid)
    }

    @Test
    fun testRsaTamperedPayloadFailsVerification() {
        val manager = SecureCore.createAsymmetricCryptoManager(
            alias = rsaAlias,
            algorithm = AsymmetricAlgorithm.RSA(keySize = 2048)
        )

        val originalPayload = "Transaction: Transfer $100 to Account B".toByteArray(Charsets.UTF_8)
        val tamperedPayload = "Transaction: Transfer $999999 to Hacker".toByteArray(Charsets.UTF_8)

        val signature = manager.sign(originalPayload)

        val isValid = manager.verify(tamperedPayload, signature)
        assertFalse("Verification must fail when payload is tampered", isValid)
    }

    @Test
    fun testEcKeyGenerationAndSignature() {
        val manager = SecureCore.createAsymmetricCryptoManager(
            alias = ecAlias,
            algorithm = AsymmetricAlgorithm.EC()
        )

        val publicKey = manager.getPublicKey()
        assertNotNull(publicKey)
        assertEquals("EC", publicKey.algorithm)

        val payload = "Critical firmware update package".toByteArray(Charsets.UTF_8)
        val signature = manager.sign(payload)

        assertTrue(signature.isNotEmpty())
        assertTrue("ECDSA signature must verify successfully", manager.verify(payload, signature))
    }

    @Test
    fun testEcDirectEncryptionThrowsIllegalStateException() {
        val manager = SecureCore.createAsymmetricCryptoManager(
            alias = ecAlias,
            algorithm = AsymmetricAlgorithm.EC()
        )

        try {
            manager.encrypt("Test payload".toByteArray())
            fail("Expected IllegalStateException for direct EC encryption")
        } catch (e: IllegalStateException) {
            assertTrue(e.message?.contains("only supported for RSA") == true)
        }
    }

    @Test
    fun testDeleteKeyLifecycle() {
        val manager = SecureCore.createAsymmetricCryptoManager(rsaAlias)
        assertNotNull(manager.getPublicKey())

        val deleted = manager.deleteKey()
        assertTrue(deleted)

        val secondDelete = manager.deleteKey()
        assertFalse("Subsequent deletion should return false", secondDelete)
    }

    @Test
    fun testRsaKeyReuseIsIdempotent() {
        // First initialization encrypts data
        val manager1 = SecureCore.createAsymmetricCryptoManager(rsaAlias)
        val originalData = "Persistent Payload".toByteArray(Charsets.UTF_8)
        val ciphertext = manager1.encrypt(originalData)

        // Second initialization with the same alias should reuse the same key pair
        val manager2 = SecureCore.createAsymmetricCryptoManager(rsaAlias)
        val decryptedData = manager2.decrypt(ciphertext)

        assertArrayEquals(originalData, decryptedData)
    }

    @Test
    fun testVerifyWithDifferentPublicKeyFails() {
        val altAlias = "test_alt_rsa_alias"
        try {
            val managerA = SecureCore.createAsymmetricCryptoManager(rsaAlias)
            val managerB = SecureCore.createAsymmetricCryptoManager(altAlias)

            val payload = "Important Contract".toByteArray(Charsets.UTF_8)
            val signatureA = managerA.sign(payload)

            // Verifying signature from Key A using Public Key from Key B must fail
            val isValid = managerA.verify(payload, signatureA, managerB.getPublicKey())
            assertFalse("Verification must fail with an unrelated public key", isValid)
        } finally {
            runCatching { SecureCore.createAsymmetricCryptoManager(altAlias).deleteKey() }
        }
    }

    @Test(expected = Exception::class)
    fun testRsaOversizedPayloadThrowsException() {
        val manager = SecureCore.createAsymmetricCryptoManager(rsaAlias)
        // 2048-bit RSA with OAEP (SHA-256) max payload is 190 bytes. 256 bytes must fail.
        val oversizedPayload = ByteArray(256) { 0x01 }
        manager.encrypt(oversizedPayload)
    }
}