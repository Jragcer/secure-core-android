package com.github.jragcer.securitycore.crypto

import com.github.jragcer.securitycore.SecureCore
import com.github.jragcer.securitycore.api.crypto.CryptoException
import com.github.jragcer.securitycore.api.crypto.CryptoManager
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.experimental.xor

@RunWith(RobolectricTestRunner::class)
class KeystoreCryptoManagerUnitTest {

    private lateinit var cryptoManager: CryptoManager

    @Before
    fun setUp(){
        cryptoManager = SecureCore.createCryptoManager("TestCryptoMasterKey")
    }

    @Test
    fun createCryptoManager_returnsNonNullInstance() {
        assertNotNull(cryptoManager)
    }

    // ==========================================
    // 1. HAPPY PATH TESTS
    // ==========================================

    @Test
    fun encryptAndDecrypt_preservesOriginalByteArray() {
        val originalText = "HardwareBackedSecurity-Payload-2026!@#$"
        val originalBytes = originalText.toByteArray(Charsets.UTF_8)

        val encrypted = cryptoManager.encrypt(originalBytes)
        assertFalse("Ciphertext should differ from original plaintext", encrypted.contentEquals(originalBytes))

        val decrypted = cryptoManager.decrypt(encrypted)
        assertArrayEquals(originalBytes, decrypted)
        assertEquals(originalText, String(decrypted, Charsets.UTF_8))
    }

    @Test
    fun encrypt_producesDifferentIVs_forSamePlaintext() {
        val plaintext = "ConsistentPlaintextInput".toByteArray()

        val encrypted1 = cryptoManager.encrypt(plaintext)
        val encrypted2 = cryptoManager.encrypt(plaintext)

        // AES-GCM requires a non-repeating random IV for every encryption call
        assertFalse("Repeated encryptions must yield different ciphertexts due to randomized IVs", encrypted1.contentEquals(encrypted2))
    }

    @Test
    fun encryptAndDecrypt_emptyByteArray_succeeds() {
        val emptyPlaintext = byteArrayOf()

        val encrypted = cryptoManager.encrypt(emptyPlaintext)
        // Encrypted data contains: 1 byte (IV length) + 12 bytes (IV) + 16 bytes (GCM tag) = 29 bytes
        assertTrue("Ciphertext for empty data must contain IV header and GCM tag", encrypted.size >= 29)

        val decrypted = cryptoManager.decrypt(encrypted)
        assertArrayEquals(emptyPlaintext, decrypted)
    }

    // ==========================================
    // 2. INTEGRITY & TAMPER RESISTANCE TESTS
    // ==========================================

    @Test
    fun decrypt_withCorruptedAuthTag_throwsDecryptionException() {
        val plaintext = "TamperResistanceCheck".toByteArray()
        val encrypted = cryptoManager.encrypt(plaintext)

        // Mutate the final byte (part of the 128-bit GCM authentication tag)
        encrypted[encrypted.lastIndex] = (encrypted[encrypted.lastIndex] + 1).toByte()

        assertThrows(CryptoException.DecryptionException::class.java) {
            cryptoManager.decrypt(encrypted)
        }
    }

    @Test
    fun decrypt_withCorruptedCiphertextBody_throwsDecryptionException() {
        val plaintext = "SensitiveBodyInformationThatCannotBeAltered".toByteArray()
        val encrypted = cryptoManager.encrypt(plaintext)

        // Mutate a byte in the middle of the ciphertext (skipping IV length and IV)
        val middleIndex = encrypted.size / 2
        encrypted[middleIndex] = (encrypted[middleIndex] xor 0x5A).toByte()

        assertThrows(CryptoException.DecryptionException::class.java) {
            cryptoManager.decrypt(encrypted)
        }
    }

    @Test
    fun decrypt_withCorruptedIvLengthHeader_throwsDecryptionException() {
        val plaintext = "HeaderValidationPayload".toByteArray()
        val encrypted = cryptoManager.encrypt(plaintext)

        // Overwrite the first byte (IV length) with an invalid length
        encrypted[0] = 0.toByte()

        assertThrows(CryptoException.DecryptionException::class.java) {
            cryptoManager.decrypt(encrypted)
        }
    }

    @Test
    fun decrypt_withTruncatedPayload_throwsDecryptionException() {
        // Less than minimal required bytes for IV header
        val truncatedPayload = byteArrayOf(12.toByte(), 1, 2, 3)

        assertThrows(CryptoException.DecryptionException::class.java) {
            cryptoManager.decrypt(truncatedPayload)
        }
    }

    @Test
    fun decrypt_withEmptyData_throwsDecryptionException() {
        assertThrows(CryptoException.DecryptionException::class.java) {
            cryptoManager.decrypt(byteArrayOf())
        }
    }

    // ==========================================
    // 3. KEY ISOLATION TESTS
    // ==========================================

    @Test
    fun decrypt_withDifferentKeyAlias_failsAuthentication() {
        val managerA = SecureCore.createCryptoManager("KeyAliasA")
        val managerB = SecureCore.createCryptoManager("KeyAliasB")

        val plaintext = "CrossKeyDecryptionShouldFail".toByteArray()
        val encryptedWithA = managerA.encrypt(plaintext)

        // Attempting to decrypt cipher generated by Key A using Key B must fail AEAD verification
        assertThrows(CryptoException.DecryptionException::class.java) {
            managerB.decrypt(encryptedWithA)
        }
    }

    // ==========================================
    // 4. STREAMING ENCRYPTION TESTS
    // ==========================================

    @Test
    fun encryptAndDecrypt_streamPreservesLargeData() {
        val largeData = ByteArray(65536) { (it % 256).toByte() } // 64 KB
        val inputStream = ByteArrayInputStream(largeData)
        val encryptedOutputStream = ByteArrayOutputStream()

        cryptoManager.encrypt(inputStream, encryptedOutputStream)
        val cipherBytes = encryptedOutputStream.toByteArray()

        val decryptInputStream = ByteArrayInputStream(cipherBytes)
        val decryptedOutputStream = ByteArrayOutputStream()

        cryptoManager.decrypt(decryptInputStream, decryptedOutputStream)
        assertArrayEquals(largeData, decryptedOutputStream.toByteArray())
    }

    @Test
    fun decrypt_tamperedStream_throwsDecryptionException() {
        val payload = "StreamTamperDetectionTestPayload".toByteArray()
        val inputStream = ByteArrayInputStream(payload)
        val encryptedOutputStream = ByteArrayOutputStream()

        cryptoManager.encrypt(inputStream, encryptedOutputStream)
        val corruptedBytes = encryptedOutputStream.toByteArray()

        // Flip a byte inside the encrypted stream content
        corruptedBytes[corruptedBytes.lastIndex] = (corruptedBytes[corruptedBytes.lastIndex] xor 0xFF.toByte()).toByte()

        val decryptInputStream = ByteArrayInputStream(corruptedBytes)
        val decryptedOutputStream = ByteArrayOutputStream()

        assertThrows(CryptoException.DecryptionException::class.java) {
            cryptoManager.decrypt(decryptInputStream, decryptedOutputStream)
        }
    }

    @Test
    fun decrypt_emptyStream_throwsDecryptionException() {
        val emptyInputStream = ByteArrayInputStream(byteArrayOf())
        val outputStream = ByteArrayOutputStream()

        assertThrows(CryptoException.DecryptionException::class.java) {
            cryptoManager.decrypt(emptyInputStream, outputStream)
        }
    }

    // ==========================================
    // 5. CONCURRENCY & THREAD SAFETY TESTS
    // ==========================================

    @Test
    fun concurrentEncryptionAndDecryption_maintainsDataIntegrity() {
        val threadCount = 8
        val iterationsPerThread = 25
        val executor = Executors.newFixedThreadPool(threadCount)
        val errors = mutableListOf<Throwable>()

        for (i in 0 until threadCount) {
            executor.submit {
                try {
                    for (j in 0 until iterationsPerThread) {
                        val message = "Thread-$i-Iteration-$j-Payload"
                        val originalBytes = message.toByteArray()

                        val encrypted = cryptoManager.encrypt(originalBytes)
                        val decrypted = cryptoManager.decrypt(encrypted)

                        if (!originalBytes.contentEquals(decrypted)) {
                            synchronized(errors) {
                                errors.add(AssertionError("Data mismatch in Thread $i iteration $j"))
                            }
                        }
                    }
                } catch (t: Throwable) {
                    synchronized(errors) {
                        errors.add(t)
                    }
                }
            }
        }

        executor.shutdown()
        val finished = executor.awaitTermination(10, TimeUnit.SECONDS)

        assertTrue("Concurrent execution timed out", finished)
        assertTrue("Encountered ${errors.size} errors during concurrent execution: $errors", errors.isEmpty())
    }
}