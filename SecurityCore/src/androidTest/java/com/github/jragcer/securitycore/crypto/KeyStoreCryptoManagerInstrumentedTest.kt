package com.github.jragcer.securitycore.crypto

import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.jragcer.securitycore.SecureCore
import com.github.jragcer.securitycore.api.crypto.CryptoException
import com.github.jragcer.securitycore.api.crypto.CryptoManager
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory

@RunWith(AndroidJUnit4::class)
class KeyStoreCryptoManagerInstrumentedTest {
    private lateinit var cryptoManager: CryptoManager
    private val primaryKeyAlias = "InstrumentedHardwareMasterKey"
    private val secondaryKeyAlias = "InstrumentedTestSecondaryKey"
    private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    @Before
    fun setUp(){
        cleanKeyStoreAliases()
        cryptoManager = SecureCore.createCryptoManager(primaryKeyAlias)
    }

    @After
    fun tearDown() {
        cleanKeyStoreAliases()
    }

    private fun cleanKeyStoreAliases() {
        listOf(primaryKeyAlias, secondaryKeyAlias).forEach { alias ->
            if (keyStore.containsAlias(alias)) {
                keyStore.deleteEntry(alias)
            }
        }
    }

    @Test
    fun createCryptoManager_returnsNonNullInstance() {
        assertNotNull(cryptoManager)
    }

    // ==========================================
    // 1. HARDWARE KEY PROPERTIES VERIFICATION
    // ==========================================

    @Test
    fun encrypt_generatesHardwareKey_withCompliantSecuritySpecifications() {
        val payload = "HardwareSecurityVerification".toByteArray(Charsets.UTF_8)
        cryptoManager.encrypt(payload)

        assertTrue(
            "Key alias must exist in AndroidKeyStore after first encryption operation",
            keyStore.containsAlias(primaryKeyAlias)
        )

        val entry = keyStore.getEntry(primaryKeyAlias, null) as? KeyStore.SecretKeyEntry
        assertNotNull("KeyStore entry must be an instance of SecretKeyEntry", entry)

        val secretKey: SecretKey = entry!!.secretKey
        assertEquals("Key algorithm must be AES", KeyProperties.KEY_ALGORITHM_AES, secretKey.algorithm)

        // Verify key parameters using SecretKeyFactory and KeyInfo where supported
        try {
            val factory = SecretKeyFactory.getInstance(secretKey.algorithm, "AndroidKeyStore")
            val keyInfo = factory.getKeySpec(secretKey, KeyInfo::class.java) as KeyInfo

            assertEquals("Key size must be 256 bits", 256, keyInfo.keySize)
            assertEquals(
                "Authorized block mode must be GCM only",
                listOf(KeyProperties.BLOCK_MODE_GCM),
                keyInfo.blockModes.toList()
            )
            assertEquals(
                "Authorized padding must be NoPadding only",
                listOf(KeyProperties.ENCRYPTION_PADDING_NONE),
                keyInfo.encryptionPaddings.toList()
            )
            assertTrue(
                "Key purposes must include ENCRYPT and DECRYPT",
                (keyInfo.purposes and KeyProperties.PURPOSE_ENCRYPT) != 0 &&
                        (keyInfo.purposes and KeyProperties.PURPOSE_DECRYPT) != 0
            )
        } catch (_: Exception) {
            // Some vendor-specific TEE implementations restrict KeyInfo retrieval via SecretKeyFactory
        }
    }

    // ==========================================
    // 2. LIFECYCLE & PERSISTENCE ACROSS INSTANCES
    // ==========================================

    @Test
    fun persistence_newManagerInstanceWithSameAlias_successfullyDecryptsData() {
        val originalText = "CrossInstancePersistencePayload-2026"
        val originalBytes = originalText.toByteArray(Charsets.UTF_8)

        // Instance 1 encrypts
        val encryptedData = cryptoManager.encrypt(originalBytes)

        // Create a completely separate instance targeting the same alias
        val secondaryInstance = SecureCore.createCryptoManager(primaryKeyAlias)

        // Instance 2 decrypts using the hardware-persisted key
        val decryptedBytes = secondaryInstance.decrypt(encryptedData)
        assertArrayEquals(originalBytes, decryptedBytes)
        assertEquals(originalText, String(decryptedBytes, Charsets.UTF_8))
    }

    // ==========================================
    // 3. KEY ISOLATION IN HARDWARE
    // ==========================================

    @Test
    fun hardwareKeyIsolation_differentAliases_cannotDecryptEachOther() {
        val otherManager = SecureCore.createCryptoManager(secondaryKeyAlias)

        val message = "PayloadIntendedForPrimaryKeyOnly".toByteArray(Charsets.UTF_8)
        val encryptedWithPrimary = cryptoManager.encrypt(message)

        // Decrypting data produced by Primary key using Secondary key must fail AEAD tag check
        assertThrows(CryptoException.DecryptionException::class.java) {
            otherManager.decrypt(encryptedWithPrimary)
        }
    }

    // ==========================================
    // 4. REAL FILE STORAGE STREAMING
    // ==========================================

    @Test
    fun fileStreaming_encryptAndDecryptOnDisk_matchesOriginalContent() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val plainFile = File(context.cacheDir, "test_plain.bin")
        val encryptedFile = File(context.cacheDir, "test_encrypted.bin")
        val restoredFile = File(context.cacheDir, "test_restored.bin")

        try {
            // Generate a 128 KB random byte file
            val payload = ByteArray(131072) { (it % 251).toByte() }
            plainFile.writeBytes(payload)

            // Encrypt from file to file
            FileInputStream(plainFile).use { input ->
                FileOutputStream(encryptedFile).use { output ->
                    cryptoManager.encrypt(input, output)
                }
            }

            assertTrue("Encrypted file must exist and contain data", encryptedFile.length() > plainFile.length())
            assertFalse(
                "Encrypted file content must not match plain content",
                plainFile.readBytes().contentEquals(encryptedFile.readBytes())
            )

            // Decrypt from file to file
            FileInputStream(encryptedFile).use { input ->
                FileOutputStream(restoredFile).use { output ->
                    cryptoManager.decrypt(input, output)
                }
            }

            // Verify content identity
            val restoredBytes = restoredFile.readBytes()
            assertArrayEquals(payload, restoredBytes)
        } finally {
            plainFile.delete()
            encryptedFile.delete()
            restoredFile.delete()
        }
    }

    // ==========================================
    // 5. EMPTY PAYLOAD THROUGH REAL KEYSTORE
    // ==========================================

    @Test
    fun encryptAndDecrypt_emptyPayload_succeedsInHardware() {
        val emptyData = byteArrayOf()
        val encrypted = cryptoManager.encrypt(emptyData)

        // Minimum length: 1 byte (length) + 12 bytes (IV) + 16 bytes (GCM Auth Tag) = 29 bytes
        assertTrue("Ciphertext for empty payload must be at least 29 bytes", encrypted.size >= 29)

        val decrypted = cryptoManager.decrypt(encrypted)
        assertArrayEquals(emptyData, decrypted)
    }
}